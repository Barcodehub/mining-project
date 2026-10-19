"""
Cargador v2 de datasets por la API REST (reemplaza a load_datasets_api.js, que validaba el esquema v1
y rechazaba valores censurados como "<0.0005").

  python evaluacion/scripts/load_api.py adapters
  python evaluacion/scripts/load_api.py measurements <archivo.json> [--out respuestas.jsonl]
  python evaluacion/scripts/load_api.py reports <archivo.json> [--out respuestas.jsonl]

Cada respuesta se guarda con el índice del registro de entrada para poder unirla con su meta
(verdad de terreno, oráculo, procedencia). Los reportes siguen el flujo real de validación:
SIN_VALIDAR (al crear) → PENDIENTE (request-validation) → VALIDADO/RECHAZADO (status).
"""
import argparse
import json
import sys
import threading
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

import requests

ROOT = Path(__file__).resolve().parents[2]
_local = threading.local()


def session():
    if not hasattr(_local, "s"):
        _local.s = requests.Session()
    return _local.s


def login(base, email="admin@admin.com", password="Admin123"):
    r = requests.post(f"{base}/api/v1/auth/login", json={"email": email, "password": password}, timeout=15)
    r.raise_for_status()
    b = r.json()
    return b.get("token") or (b.get("data") or {}).get("token")


def create_adapters(base, headers):
    for f in sorted((ROOT / "evaluacion/real/adapters").glob("*.json")):
        a = json.loads(f.read_text(encoding="utf-8"))
        r = requests.post(f"{base}/api/v1/adapters", headers=headers, json=a, timeout=15)
        print(f"adapter {a['sensorType']}: HTTP {r.status_code}")
        r.raise_for_status()


def post_measurement(base, headers, idx, rec):
    r = session().post(f"{base}/api/v1/environmental-data", headers=headers,
                       json={"sensorType": rec["sensorType"], "payload": rec["payload"]}, timeout=30)
    body = r.json() if r.headers.get("content-type", "").startswith("application/json") else {"raw": r.text[:300]}
    return {"i": idx, "status": r.status_code, "body": body}


def post_report(base, headers, idx, rec):
    s = session()
    r = s.post(f"{base}/api/v1/posts", headers=headers, json=rec["post"], timeout=30)
    out = {"i": idx, "status": r.status_code, "id": None, "final": None}
    if r.status_code != 201:
        out["error"] = r.text[:300]
        return out
    rid = r.json()["id"]
    out["id"] = rid
    target = rec["workflow"]["targetStatus"]
    if target in ("PENDIENTE", "VALIDADO", "RECHAZADO"):
        r = s.patch(f"{base}/api/v1/reports/{rid}/request-validation", headers=headers, timeout=30)
        if r.status_code != 200:
            out["error"] = f"request-validation {r.status_code}"
            return out
    if target in ("VALIDADO", "RECHAZADO"):
        r = s.patch(f"{base}/api/v1/reports/{rid}/status", headers=headers,
                    json={"status": target, "validatorId": rec["workflow"].get("validatorId", 1)}, timeout=30)
        if r.status_code != 200:
            out["error"] = f"status {r.status_code}"
            return out
    out["final"] = target
    return out


def run(kind, path, base, out, workers):
    headers = {"Authorization": f"Bearer {login(base)}", "Content-Type": "application/json"}
    if kind == "adapters":
        create_adapters(base, headers)
        return
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    if isinstance(data, dict) and "items" in data:
        data = data["items"]
    fn = post_measurement if kind == "measurements" else post_report
    with ThreadPoolExecutor(max_workers=workers) as ex:
        results = list(ex.map(lambda t: fn(base, headers, t[0], t[1]), enumerate(data)))
    ok = sum(1 for r in results if r["status"] == 201 and not r.get("error"))
    print(f"{kind}: {ok}/{len(results)} correctos ({path})")
    if out:
        Path(out).parent.mkdir(parents=True, exist_ok=True)
        with open(out, "w", encoding="utf-8") as f:
            for r in results:
                f.write(json.dumps(r, ensure_ascii=False) + "\n")
    bad = [r for r in results if r["status"] != 201 or r.get("error")]
    for r in bad[:5]:
        print("  error:", r)
    return results


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    ap = argparse.ArgumentParser()
    ap.add_argument("kind", choices=["adapters", "measurements", "reports"])
    ap.add_argument("path", nargs="?")
    ap.add_argument("--base", default="http://localhost:8082")
    ap.add_argument("--out")
    ap.add_argument("--workers", type=int, default=8)
    a = ap.parse_args()
    run(a.kind, a.path, a.base, a.out, a.workers)


if __name__ == "__main__":
    main()
