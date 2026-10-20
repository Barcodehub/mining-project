"""
Orquestador de experimentos. Cada corrida usa un entorno Docker LIMPIO (compose down -v / up).

  python evaluacion/run_experiments.py functional                  # datos reales, inyección de eventos, fidelidad y base de reportes
  python evaluacion/run_experiments.py v3                          # escenarios de reportes (aligned, null, noisy*)
  python evaluacion/run_experiments.py perf --reps 3 --scenarios bajo,medio,alto
  python evaluacion/run_experiments.py all

Resultados: evaluacion/results/<fecha>/<experimento>/...
"""
import argparse
import csv
import json
import platform
import subprocess
import sys
import threading
import time
from datetime import datetime
from pathlib import Path

import requests

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "evaluacion/synthetic"))
sys.path.insert(0, str(ROOT / "evaluacion/scripts"))
from real_canonical import real_canonical  # noqa: E402
import load_api  # noqa: E402

MGMT, AUDIT, USERS = "http://localhost:8082", "http://localhost:8083", "http://localhost:8081"
SERVICES = ["zookeeper", "kafka", "mongo", "postgres-audit", "mining-users", "mining-management",
            "mining-audit-notifications"]
DATA = ROOT / "evaluacion/data/v2"
REAL = ROOT / "evaluacion/real/processed"


def sh(cmd, check=True, capture=False, timeout=None):
    print("  $", " ".join(cmd) if isinstance(cmd, list) else cmd, flush=True)
    r = subprocess.run(cmd, cwd=ROOT, check=check, text=True, capture_output=capture, timeout=timeout,
                       encoding="utf-8", errors="replace")
    return r.stdout if capture else None


def compose(project):
    return ["docker", "compose", "-p", project, "-f", "docker-compose.yml"]


def env_up(project):
    sh(compose(project) + ["down", "-v", "--remove-orphans"], check=False, capture=True)
    sh(compose(project) + ["up", "-d", "--build"] + SERVICES, capture=True)
    t0 = time.time()
    while time.time() - t0 < 420:
        try:
            if all(requests.get(f"{u}/actuator/health", timeout=3).status_code == 200 for u in (MGMT, AUDIT, USERS)):
                time.sleep(5)  # consumidores Kafka asignados
                print(f"  entorno listo en {time.time() - t0:.0f}s", flush=True)
                return
        except requests.RequestException:
            pass
        time.sleep(4)
    raise RuntimeError("el entorno no quedó saludable")


def env_down(project):
    sh(compose(project) + ["down", "-v", "--remove-orphans"], check=False, capture=True)


def write_json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=1), encoding="utf-8")


def load(kind, data, out, workers=8):
    tmp = out.with_suffix(".input.json")
    write_json(tmp, data)
    res = load_api.run(kind, str(tmp), MGMT, str(out), workers)
    tmp.unlink()
    return res


def scrape(base):
    return requests.get(f"{base}/actuator/prometheus", timeout=10).text


def metric_sum(text, name):
    tot = 0.0
    for line in text.splitlines():
        if line.startswith(name + "{") or line.startswith(name + " "):
            try:
                tot += float(line.rsplit(" ", 1)[1])
            except ValueError:
                pass
    return tot


def drain(timeout=180):
    """Espera a que el consumidor procese todas las alertas publicadas."""
    t0 = time.time()
    pub = cons = 0
    while time.time() - t0 < timeout:
        pub = metric_sum(scrape(MGMT), "environmental_alerts_published_total")
        a = scrape(AUDIT)
        cons = metric_sum(a, "alert_consumed_total") + metric_sum(a, "alert_duplicates_discarded_total")
        if cons >= pub:
            return pub, cons, time.time() - t0
        time.sleep(2)
    return pub, cons, time.time() - t0


def environment_info():
    info = {"timestamp": datetime.now().isoformat(), "platform": platform.platform(),
            "python": platform.python_version()}
    try:
        info["docker"] = sh(["docker", "version", "--format", "{{.Server.Version}}"], capture=True).strip()
        info["docker_info"] = sh(["docker", "info", "--format", "{{.NCPU}} CPU, {{.MemTotal}} B"], capture=True).strip()
        info["git_commit"] = sh(["git", "rev-parse", "HEAD"], capture=True).strip()
    except Exception as e:  # noqa: BLE001
        info["error"] = str(e)
    return info


# ── Experimentos ───────────────────────────────────────────────────────────
def functional(out):
    project = "exp-func-v2"
    out.mkdir(parents=True, exist_ok=True)
    env_up(project)
    try:
        canon = real_canonical()
        inj = json.loads((DATA / "inyeccion_eventos.json").read_text(encoding="utf-8"))
        load_api.run("adapters", None, MGMT, None, 1)
        load("measurements", json.loads((REAL / "cdmb_api.json").read_text(encoding="utf-8")), out / "real_cdmb_raw.jsonl")
        load("measurements", json.loads((REAL / "corponor_api.json").read_text(encoding="utf-8")), out / "real_corponor_raw.jsonl")
        load("measurements", canon, out / "real_canonical.jsonl")
        load("measurements", inj, out / "injection.jsonl")
        pub, cons, waited = drain()
        write_json(out / "drain.json", {"published": pub, "consumed": cons, "seconds": waited})
        (out / "audit_prometheus.txt").write_text(scrape(AUDIT), encoding="utf-8")
    finally:
        env_down(project)


def v3(out):
    meas = json.loads((DATA / "mediciones_sinteticas.json").read_text(encoding="utf-8"))
    for scen in ("aligned", "null", "noisy10", "noisy20", "noisy30", "noisy50"):
        d = out / scen
        d.mkdir(parents=True, exist_ok=True)
        env_up("exp-v3")
        try:
            load("measurements", meas, d / "measurements.jsonl")
            load("reports", json.loads((DATA / f"denuncias_{scen}.json").read_text(encoding="utf-8")), d / "reports.jsonl")
            token = load_api.login(MGMT)
            h = {"Authorization": f"Bearer {token}"}
            res = {ep: requests.get(f"{MGMT}/api/v1/analytics/{ep}", headers=h, timeout=60).json()
                   for ep in ("clusters", "reports", "fusion", "correlation")}
            write_json(d / "analytics.json", res)
            print(f"  {scen}: rho={res['correlation'].get('spearmanCoefficient')} p={res['correlation'].get('pValue')}")
        finally:
            env_down("exp-v3")


class StatsSampler(threading.Thread):
    def __init__(self, path, period=5):
        super().__init__(daemon=True)
        self.path, self.period, self.stop = path, period, threading.Event()

    def run(self):
        with open(self.path, "w", newline="", encoding="utf-8") as f:
            w = csv.writer(f)
            w.writerow(["t", "container", "cpu_perc", "mem_usage"])
            while not self.stop.is_set():
                try:
                    o = subprocess.run(["docker", "stats", "--no-stream", "--format", "{{.Name}};{{.CPUPerc}};{{.MemUsage}}"],
                                       capture_output=True, text=True, timeout=30).stdout
                    t = time.time()
                    for line in o.strip().splitlines():
                        w.writerow([f"{t:.0f}"] + line.split(";"))
                    f.flush()
                except Exception:  # noqa: BLE001
                    pass
                self.stop.wait(self.period)


def perf(reps, scenarios, out):
    meas = json.loads((DATA / "mediciones_sinteticas.json").read_text(encoding="utf-8"))
    real = json.loads((REAL / "cdmb_api.json").read_text(encoding="utf-8")) + \
        json.loads((REAL / "corponor_api.json").read_text(encoding="utf-8"))
    reports = json.loads((DATA / "denuncias_aligned.json").read_text(encoding="utf-8"))
    for r in range(1, reps + 1):
        for scen in scenarios:
            d = out / f"{scen}_r{r}"
            d.mkdir(parents=True, exist_ok=True)
            print(f"\n=== perf {scen} repetición {r} ===", flush=True)
            env_up("exp-perf")
            try:
                load_api.run("adapters", None, MGMT, None, 1)
                load("measurements", meas, d / "preload_measurements.jsonl")
                load("measurements", real, d / "preload_real.jsonl")
                load("reports", reports, d / "preload_reports.jsonl")
                drain()
                (d / "audit_before.txt").write_text(scrape(AUDIT), encoding="utf-8")
                (d / "mgmt_before.txt").write_text(scrape(MGMT), encoding="utf-8")
                sampler = StatsSampler(d / "docker_stats.csv")
                sampler.start()
                rel = d.relative_to(ROOT / "evaluacion/results").as_posix()
                t0 = time.time()
                sh(compose("exp-perf") + ["run", "--rm", "--no-deps", "-e", f"ESCENARIO={scen}",
                                          "-e", f"RESULT_FILE=/results/{rel}/k6_summary.json",
                                          "--entrypoint", "k6", "k6", "run", "--quiet", "-e", f"ESCENARIO={scen}",
                                          "-e", f"RESULT_FILE=/results/{rel}/k6_summary.json",
                                          "/k6/load_test_v2.js"],
                   check=False, timeout=3600)
                k6_seconds = time.time() - t0
                pub, cons, waited = drain()
                sampler.stop.set()
                sampler.join(timeout=40)
                (d / "audit_after.txt").write_text(scrape(AUDIT), encoding="utf-8")
                (d / "mgmt_after.txt").write_text(scrape(MGMT), encoding="utf-8")
                write_json(d / "run.json", {"scenario": scen, "rep": r, "k6_seconds": k6_seconds,
                                             "alerts_published_total": pub, "alerts_consumed_total": cons,
                                             "drain_seconds": waited})
            finally:
                env_down("exp-perf")


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    ap = argparse.ArgumentParser()
    ap.add_argument("experiment", choices=["functional", "v3", "perf", "all"])
    ap.add_argument("--reps", type=int, default=3)
    ap.add_argument("--scenarios", default="bajo,medio,alto")
    ap.add_argument("--tag", default=datetime.now().strftime("%Y-%m-%d"))
    a = ap.parse_args()
    out = ROOT / "evaluacion/results" / a.tag
    out.mkdir(parents=True, exist_ok=True)
    write_json(out / f"environment_{a.experiment}_v2.json", environment_info())
    if a.experiment in ("functional", "all"):
        functional(out / "functional_v2")
    if a.experiment in ("v3", "all"):
        v3(out / "v3")
    if a.experiment in ("perf", "all"):
        perf(a.reps, a.scenarios.split(","), out / "perf")


if __name__ == "__main__":
    main()
