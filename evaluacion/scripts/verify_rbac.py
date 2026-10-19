"""
Verificación end-to-end del control de acceso por roles en un entorno Docker limpio (proyecto rbac-check).

  python evaluacion/scripts/verify_rbac.py

Crea un INGENIERO, un MODERADOR y un CLIENTE, y comprueba quién puede ingerir, registrar adapters, validar
reportes y consultar la auditoría; verifica además que el validador guardado sea el del token y no el del cuerpo.
"""
import base64
import json
import subprocess
import sys
from pathlib import Path

import requests

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "evaluacion"))
import run_experiments as rx  # noqa: E402

M, U = rx.MGMT, rx.USERS


def login(email, pw):
    r = requests.post(f"{M}/api/v1/auth/login", json={"email": email, "password": pw}, timeout=15)
    r.raise_for_status()
    b = r.json()
    return b.get("token") or (b.get("data") or {}).get("token")


def H(t):
    return {"Authorization": f"Bearer {t}", "Content-Type": "application/json"}


def user(n, role_id=None, token=None):
    body = {"name": f"U{n}", "lastName": "Test", "password": "Clave12345", "email": f"u{n}@test.co",
            "identificationDocument": f"10{n:08d}", "phoneNumber": "+573000000000", "birthDate": "1995-01-01"}
    if role_id:
        body["roleId"] = role_id
        r = requests.post(f"{U}/users", headers=H(token), json=body, timeout=15)
    else:
        body["roleId"] = 4
        r = requests.post(f"{U}/users/public", json=body, timeout=15)
    assert r.status_code in (200, 201), (r.status_code, r.text[:300])
    return body["email"], body["password"]


def main():
    rx.env_up("rbac-check")
    checks = []
    try:
        adm = login("admin@admin.com", "Admin123")
        ing = login(*user(1, 2, adm))
        mod = login(*user(2, 3, ing))
        cli = login(*user(3))
        rec = json.loads((ROOT / "evaluacion/data/v2/denuncias_aligned.json").read_text(encoding="utf-8"))[0]
        meas = json.loads((ROOT / "evaluacion/data/v2/mediciones_sinteticas.json").read_text(encoding="utf-8"))[0]
        adapter = json.loads(next((ROOT / "evaluacion/real/adapters").glob("*.json")).read_text(encoding="utf-8"))

        def chk(name, r, want):
            checks.append((name, r.status_code, want, r.status_code == want))

        r = requests.post(f"{M}/api/v1/posts", headers=H(cli), json=rec["post"], timeout=15)
        chk("CLIENTE crea reporte", r, 201)
        rid = r.json()["id"]
        chk("CLIENTE solicita validación", requests.patch(f"{M}/api/v1/reports/{rid}/request-validation", headers=H(cli), timeout=15), 200)
        body = {"status": "VALIDADO", "validatorId": 999}
        chk("CLIENTE valida (debe negarse)", requests.patch(f"{M}/api/v1/reports/{rid}/status", headers=H(cli), json=body, timeout=15), 403)
        chk("INGENIERO valida (debe negarse)", requests.patch(f"{M}/api/v1/reports/{rid}/status", headers=H(ing), json=body, timeout=15), 403)
        r = requests.patch(f"{M}/api/v1/reports/{rid}/status", headers=H(mod), json=body, timeout=15)
        chk("MODERADOR valida", r, 200)
        vid = r.json().get("validatorId")
        payload = {"sensorType": meas["sensorType"], "payload": meas["payload"]}
        chk("CLIENTE ingiere medición (debe negarse)", requests.post(f"{M}/api/v1/environmental-data", headers=H(cli), json=payload, timeout=15), 403)
        chk("INGENIERO ingiere medición", requests.post(f"{M}/api/v1/environmental-data", headers=H(ing), json=payload, timeout=15), 201)
        chk("CLIENTE registra adapter (debe negarse)", requests.post(f"{M}/api/v1/adapters", headers=H(cli), json=adapter, timeout=15), 403)
        chk("INGENIERO registra adapter", requests.post(f"{M}/api/v1/adapters", headers=H(ing), json=adapter, timeout=15), 201)
        chk("MODERADOR consulta auditoría (debe negarse)", requests.get(f"{M}/api/v1/audit", headers=H(mod), timeout=15), 403)
        chk("ADMINISTRADOR consulta auditoría", requests.get(f"{M}/api/v1/audit", headers=H(adm), timeout=15), 200)
        r = requests.get(f"{M}/api/v1/reports/{rid}", headers=H(cli), timeout=15).json()
        print("CLIENTE ve ubicación:", r.get("latitude"), r.get("longitude"), "| validatorId en respuesta:", vid)
        p = mod.split(".")[1]
        mod_id = json.loads(base64.urlsafe_b64decode(p + "=" * (-len(p) % 4)))["userId"]
        q = ("SELECT string_agg(table_name, ',') FROM information_schema.columns WHERE column_name='validated_by'")
        psql = rx.compose("rbac-check") + ["exec", "-T", "postgres-audit", "psql", "-U", "postgres", "-d", "mining_management", "-tAc"]
        tbl = subprocess.run(psql + [q], capture_output=True, text=True).stdout.strip()
        got = subprocess.run(psql + [f"SELECT validated_by FROM {tbl} WHERE id={rid}"], capture_output=True, text=True).stdout.strip()
        checks.append((f"validated_by = id del token del MODERADOR ({mod_id}), no el 999 del cuerpo", got, str(mod_id), got == str(mod_id)))
    finally:
        rx.env_down("rbac-check")
    for c in checks:
        print(("OK  " if c[3] else "FAIL"), f"{c[0]}: HTTP {c[1]} (esperado {c[2]})")
    print("todas OK" if all(c[3] for c in checks) else "HAY FALLOS")


if __name__ == "__main__":
    main()
