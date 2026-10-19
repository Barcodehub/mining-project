"""
Resume las corridas de rendimiento (evaluacion/results/<tag>/perf/<escenario>_r<n>/).

Latencia de punta a punta (alert_e2e_latency): se usa la DIFERENCIA de los histogramas acumulados
de Prometheus (después − antes de k6), de modo que solo cuentan las alertas generadas durante la
prueba de carga. Los cuantiles se interpolan linealmente dentro del bucket (igual que histogram_quantile).

Uso: python evaluacion/analysis/perf_summary.py [--tag 2026-10-19]
Salida: tabla en consola y evaluacion/results/<tag>/perf/summary.json
"""
import argparse
import json
import re
import statistics
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BUCKET = re.compile(r'^(\w+)_bucket\{(.*?)le="([^"]+)".*?\}\s+([0-9.eE+-]+)$')


def buckets(text, name):
    out = {}
    for line in text.splitlines():
        m = BUCKET.match(line)
        if m and m.group(1) == name:
            le = float("inf") if m.group(3) == "+Inf" else float(m.group(3))
            out[le] = out.get(le, 0.0) + float(m.group(4))
    return out


def scalar(text, name):
    tot = 0.0
    for line in text.splitlines():
        if line.startswith(name + "{") or line.startswith(name + " "):
            tot += float(line.rsplit(" ", 1)[1])
    return tot


def quantile(b, q):
    les = sorted(b)
    total = b[les[-1]]
    if total <= 0:
        return None
    rank = q * total
    prev_le, prev_c = 0.0, 0.0
    for le in les:
        c = b[le]
        if c >= rank:
            if le == float("inf"):
                return prev_le
            frac = 0 if c == prev_c else (rank - prev_c) / (c - prev_c)
            return prev_le + (le - prev_le) * frac
        prev_le, prev_c = le, c
    return les[-2]


def run_summary(d):
    k6 = json.loads((d / "k6_summary.json").read_text(encoding="utf-8"))["metrics"]
    before, after = (d / "audit_before.txt").read_text(encoding="utf-8"), (d / "audit_after.txt").read_text(encoding="utf-8")
    mb, ma = (d / "mgmt_before.txt").read_text(encoding="utf-8"), (d / "mgmt_after.txt").read_text(encoding="utf-8")
    bb, ba = buckets(before, "alert_e2e_latency_seconds"), buckets(after, "alert_e2e_latency_seconds")
    diff = {le: ba.get(le, 0) - bb.get(le, 0) for le in ba}
    n_alerts = scalar(after, "alert_e2e_latency_seconds_count") - scalar(before, "alert_e2e_latency_seconds_count")
    s_alerts = scalar(after, "alert_e2e_latency_seconds_sum") - scalar(before, "alert_e2e_latency_seconds_sum")
    pub = scalar(ma, "environmental_alerts_published_total") - scalar(mb, "environmental_alerts_published_total")
    failed = scalar(ma, "environmental_alerts_publish_failed_total") - scalar(mb, "environmental_alerts_publish_failed_total")

    def tv(name, stat):
        m = k6.get(name)
        return None if m is None else m["values"].get(stat)

    run = json.loads((d / "run.json").read_text(encoding="utf-8"))
    dur = run["k6_seconds"]
    return {
        "iterations": tv("iterations", "count"),
        "http_reqs": tv("http_reqs", "count"),
        "req_per_s": tv("http_reqs", "rate"),
        "http_failed_rate": tv("http_req_failed", "rate"),
        "checks_rate": tv("checks", "rate"),
        "checks_passes": k6.get("checks", {}).get("values", {}).get("passes"),
        "checks_fails": k6.get("checks", {}).get("values", {}).get("fails"),
        "post_med_p50": tv("post_medicion_duration_ms", "med"), "post_med_p95": tv("post_medicion_duration_ms", "p(95)"),
        "post_med_p99": tv("post_medicion_duration_ms", "p(99)"),
        "post_rep_p50": tv("post_report_duration_ms", "med"), "post_rep_p95": tv("post_report_duration_ms", "p(95)"),
        "get_med_p50": tv("get_mediciones_duration_ms", "med"), "get_med_p95": tv("get_mediciones_duration_ms", "p(95)"),
        "get_rep_p50": tv("get_reports_duration_ms", "med"), "get_rep_p95": tv("get_reports_duration_ms", "p(95)"),
        "http_p50": tv("http_req_duration", "med"), "http_p95": tv("http_req_duration", "p(95)"),
        "http_p99": tv("http_req_duration", "p(99)"),
        "measurements_created": tv("mediciones_creadas_total", "count"),
        "meas_iot": tv("mediciones_fuente_iot_total", "count"), "meas_cdmb": tv("mediciones_fuente_cdmb_total", "count"),
        "meas_corponor": tv("mediciones_fuente_corponor_total", "count"),
        "publishable_client": tv("mediciones_publicables_total", "count"),
        "alerts_published": pub, "alerts_publish_failed": failed, "alerts_e2e_count": n_alerts,
        "alerts_per_s": n_alerts / dur if dur else None,
        "e2e_mean_ms": 1000 * s_alerts / n_alerts if n_alerts else None,
        "e2e_p50_ms": None if not n_alerts else 1000 * quantile(diff, 0.50),
        "e2e_p95_ms": None if not n_alerts else 1000 * quantile(diff, 0.95),
        "e2e_p99_ms": None if not n_alerts else 1000 * quantile(diff, 0.99),
        "drain_seconds": run["drain_seconds"],
    }


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    ap = argparse.ArgumentParser()
    ap.add_argument("--tag", default="2026-10-19")
    a = ap.parse_args()
    base = ROOT / "evaluacion/results" / a.tag / "perf"
    runs = {}
    for d in sorted(base.glob("*_r*")):
        if (d / "k6_summary.json").exists() and (d / "run.json").exists():
            runs[d.name] = run_summary(d)
    agg = {}
    for scen in ("bajo", "medio", "alto"):
        rs = [v for k, v in runs.items() if k.startswith(scen + "_")]
        if not rs:
            continue
        agg[scen] = {"n_reps": len(rs)}
        for key in rs[0]:
            vals = [r[key] for r in rs if isinstance(r.get(key), (int, float))]
            if vals:
                agg[scen][key] = {"mean": statistics.mean(vals), "sd": statistics.stdev(vals) if len(vals) > 1 else 0.0,
                                  "values": vals}
    (base / "summary.json").write_text(json.dumps({"runs": runs, "aggregate": agg}, indent=1), encoding="utf-8")
    keys = ["iterations", "req_per_s", "http_failed_rate", "checks_rate", "post_med_p50", "post_med_p95",
            "e2e_p50_ms", "e2e_p95_ms", "e2e_p99_ms", "alerts_per_s", "alerts_published", "alerts_e2e_count"]
    for name, r in runs.items():
        print(name, " ".join(f"{k}={r[k]:.4g}" if isinstance(r[k], (int, float)) else f"{k}={r[k]}" for k in keys))


if __name__ == "__main__":
    main()
