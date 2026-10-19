"""
Generador de datos sintéticos v2, anclado a observaciones reales.

Principio: no se inventan rangos. Cada medición sintética es un **bootstrap estratificado** de una
observación real (CDMB 2024–2025 o CORPONOR 2025) con perturbación multiplicativa del orden de la
incertidumbre analítica. Los valores censurados ("<LD") se conservan tal cual.

Estratos (criterio geográfico, ver evaluacion/real/scripts/prepare_cdmb.py):
  H ("alta exposición") : estaciones del distrito minero Vetas–California y aguas abajo (Vetas, Suratá)
  B ("base")            : resto de estaciones con metales (CDMB y CORPONOR)

Clústeres sintéticos SIM-A … SIM-G: verdad de terreno conocida p_i = fracción de mediciones
muestreadas del estrato H. Los nombres son deliberadamente abstractos: no representan lugares reales.

Reportes ciudadanos (V3), escenarios:
  aligned  : intensidad y tasa de validación crecen con p_i
  null     : mismos conteos y estados, pero permutados entre clústeres (se rompe la asociación)
  noisy_r  : aligned, con una fracción r de reportes reasignados a un clúster y estado al azar

Uso:  python evaluacion/synthetic/generator.py           (escribe los archivos de evaluacion/data/v2)
"""
import json
import math
import random
from datetime import datetime, timedelta, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
REAL = ROOT / "evaluacion/real/processed"
OUT = ROOT / "evaluacion/data/v2"
SEED = 20261019

CLUSTERS = [
    # id, p (fracción del estrato H), n mediciones, centro (lat, lon)
    ("SIM-A", 0.85, 220, (7.35, -72.92)),
    ("SIM-B", 0.70, 180, (7.42, -72.80)),
    ("SIM-C", 0.55, 150, (7.20, -73.02)),
    ("SIM-D", 0.40, 120, (7.60, -72.75)),
    ("SIM-E", 0.25, 160, (7.88, -72.52)),
    ("SIM-F", 0.10, 100, (7.10, -73.15)),
    ("SIM-G", 0.00, 70, (7.95, -72.60)),
]
CLUSTER_RADIUS_DEG = 0.03
TOTAL_REPORTS = 500

# Perturbación (σ en escala log o absoluta) ~ incertidumbre analítica/de campo típica
JITTER = {"hg_total": ("log", 0.15), "cyanide": ("log", 0.15), "arsenic": ("log", 0.15),
          "turbidity": ("log", 0.20), "dissolved_oxygen": ("log", 0.05),
          "ph": ("abs", 0.10), "temperature": ("abs", 0.50)}
CANONICAL = ["ph", "temperature", "turbidity", "dissolved_oxygen", "hg_total", "cyanide", "arsenic"]


# ── Pools reales en unidades canónicas ─────────────────────────────────────
def _scale(raw, factor):
    """Convierte respetando '<': devuelve float, '<x' o None."""
    if raw is None:
        return None
    s = str(raw).strip()
    if s in ("", "-999"):
        return None
    pre = "<" if s.startswith("<") else ""
    num = s[1:].strip() if pre else s
    try:
        v = float(num.replace(",", ".")) * factor
    except ValueError:
        return None
    return f"<{v:.6g}" if pre else v


def load_pools():
    cdmb = json.loads((REAL / "cdmb_api.json").read_text(encoding="utf-8"))
    corp = json.loads((REAL / "corponor_api.json").read_text(encoding="utf-8"))
    pools = {"H": [], "B": []}
    for r in cdmb:
        if not r["meta"]["has_metals"]:
            continue
        p = r["payload"]
        rec = {"ph": _scale(p.get("ph_unidades_de_ph"), 1), "temperature": _scale(p.get("t_muestra_c"), 1),
               "turbidity": _scale(p.get("turbiedad_ntu"), 1),
               "dissolved_oxygen": _scale(p.get("oxigeno_disuelto_mg_l"), 1),
               "hg_total": _scale(p.get("mercurio_mg_l"), 1000), "cyanide": _scale(p.get("cianuro_mg_l"), 1),
               "arsenic": _scale(p.get("arsenico_mg_l"), 1), "altitude": _scale(p.get("altura"), 1),
               "_src": f"CDMB:{r['meta']['station']}:{r['meta']['row']}"}
        pools["H" if r["meta"]["categoria"] in ("DISTRITO_MINERO", "AGUAS_ABAJO") else "B"].append(rec)
    for r in corp:
        p = r["payload"]
        rec = {"ph": _scale(p["campo"]["ph"], 1), "temperature": _scale(p["campo"]["temperatura_c"], 1),
               "turbidity": _scale(p["campo"]["turbiedad_ntu"], 1),
               "dissolved_oxygen": _scale(p["campo"]["oxigeno_mg_l"], 1),
               "hg_total": _scale(p["laboratorio"]["hg_total_mg_l"], 1000), "cyanide": None,
               "arsenic": _scale(p["laboratorio"]["as_total_mg_l"], 1), "altitude": p["punto"]["msnm"],
               "_src": f"CORPONOR:{p['estacion']}"}
        pools["B"].append(rec)
    return pools


def _jitter(name, v, rnd):
    if not isinstance(v, float):
        return v                      # censurado ('<x') o ausente se conservan
    kind, s = JITTER.get(name, ("abs", 0.0))
    if kind == "log":
        out = v * math.exp(rnd.gauss(0, s)) if v > 0 else v
    else:
        out = v + rnd.gauss(0, s)
    if name == "ph":
        out = min(14.0, max(0.0, out))
    return round(out, 6)


def _point(center, rnd):
    return (round(center[0] + rnd.uniform(-CLUSTER_RADIUS_DEG, CLUSTER_RADIUS_DEG), 6),
            round(center[1] + rnd.uniform(-CLUSTER_RADIUS_DEG, CLUSTER_RADIUS_DEG), 6))


def _ts(rnd, start=datetime(2025, 1, 1, tzinfo=timezone.utc), days=540):
    return (start + timedelta(seconds=rnd.uniform(0, days * 86400))).strftime("%Y-%m-%dT%H:%M:%SZ")


def make_measurements(pools, seed=SEED, clusters=CLUSTERS):
    rnd = random.Random(seed)
    out = []
    for cid, p, n, center in clusters:
        for i in range(n):
            stratum = "H" if rnd.random() < p else "B"
            src = rnd.choice(pools[stratum])
            lat, lon = _point(center, rnd)
            payload = {"deviceId": f"{cid}-S{i % 12 + 1:02d}", "latitude": lat, "longitude": lon,
                       "clusterId": cid, "altitude": src["altitude"], "timestamp": _ts(rnd)}
            for k in CANONICAL:
                v = _jitter(k, src.get(k), rnd)
                if v is not None:
                    payload[k] = v
            out.append({"sensorType": "multi_parameter_sensor", "payload": payload,
                        "meta": {"cluster": cid, "p": p, "stratum": stratum, "source": src["_src"]}})
    return out


# ── Reportes ciudadanos ────────────────────────────────────────────────────
STATUSES = ["VALIDADO", "RECHAZADO", "PENDIENTE", "SIN_VALIDAR"]
CONTENT = ["Agua turbia y con olor químico en la quebrada.", "Maquinaria operando en la ribera sin señalización.",
           "Peces muertos aguas abajo del entable.", "Vertimiento de lodos grises al cauce.",
           "Ruido de molinos de noche cerca del río.", "Coloración amarillenta del agua tras la lluvia."]


def status_probs(p):
    """P(VALIDADO, RECHAZADO, PENDIENTE, SIN_VALIDAR) en función de la exposición p (suman 1)."""
    return [0.15 + 0.55 * p, 0.35 - 0.30 * p, 0.25, 0.25 - 0.25 * p]


def make_reports(scenario="aligned", noise=0.0, seed=SEED, clusters=CLUSTERS, total=TOTAL_REPORTS):
    rnd = random.Random(seed + 7)
    weights = [0.3 + p for _, p, _, _ in clusters]
    counts = [round(total * w / sum(weights)) for w in weights]
    counts[0] += total - sum(counts)
    ps = [p for _, p, _, _ in clusters]
    if scenario == "null":
        perm = list(range(len(clusters)))
        rnd.shuffle(perm)
        counts = [counts[j] for j in perm]
        ps = [ps[j] for j in perm]          # estados según la exposición de OTRO clúster
    mean_p = sum(p for _, p, _, _ in clusters) / len(clusters)
    out = []
    k = 0
    for idx, (cid, _, _, center) in enumerate(clusters):
        for _ in range(counts[idx]):
            target_idx, p_eff = idx, ps[idx]
            if scenario == "noisy" and rnd.random() < noise:
                target_idx, p_eff = rnd.randrange(len(clusters)), mean_p
            tcid, tcenter = clusters[target_idx][0], clusters[target_idx][3]
            status = rnd.choices(STATUSES, weights=status_probs(p_eff))[0]
            lat, lon = _point(tcenter, rnd)
            k += 1
            out.append({"post": {"title": f"Reporte {tcid} #{k}", "content": rnd.choice(CONTENT),
                                 "userId": rnd.randint(2, 200), "type": "REPORT", "latitude": lat,
                                 "longitude": lon, "clusterId": tcid, "images": []},
                        "workflow": {"targetStatus": status, "validatorId": 1},
                        "meta": {"cluster": tcid, "scenario": scenario, "noise": noise}})
    return out


def cri_by_cluster(reports, clusters=CLUSTERS):
    """CRI con los mismos pesos que la API (VALIDADO 1, PENDIENTE 0.5, SIN_VALIDAR 0.25, RECHAZADO 0)."""
    w = {"VALIDADO": 1.0, "PENDIENTE": 0.5, "SIN_VALIDAR": 0.25, "RECHAZADO": 0.0}
    acc = {c[0]: [0, 0.0] for c in clusters}
    for r in reports:
        a = acc[r["post"]["clusterId"]]
        a[0] += 1
        a[1] += w[r["workflow"]["targetStatus"]]
    return {c: (v[1] / v[0] if v[0] else 0.0) for c, v in acc.items()}


def write_all():
    OUT.mkdir(parents=True, exist_ok=True)
    pools = load_pools()
    meas = make_measurements(pools)
    (OUT / "mediciones_sinteticas.json").write_text(json.dumps(meas, ensure_ascii=False, indent=1), encoding="utf-8")
    scen = {"aligned": make_reports("aligned"), "null": make_reports("null")}
    for r in (0.1, 0.2, 0.3, 0.5):
        scen[f"noisy{int(r * 100)}"] = make_reports("noisy", noise=r)
    for name, reps in scen.items():
        (OUT / f"denuncias_{name}.json").write_text(json.dumps(reps, ensure_ascii=False, indent=1), encoding="utf-8")
    truth = {"seed": SEED, "strata": {k: len(v) for k, v in pools.items()},
             "clusters": [{"id": c, "p": p, "n": n, "center": ctr} for c, p, n, ctr in CLUSTERS],
             "cri_expected": {name: cri_by_cluster(reps) for name, reps in scen.items()}}
    (OUT / "clusters_ground_truth.json").write_text(json.dumps(truth, ensure_ascii=False, indent=1), encoding="utf-8")
    return pools, meas, scen


if __name__ == "__main__":
    import sys
    sys.stdout.reconfigure(encoding="utf-8")
    pools, meas, scen = write_all()
    print("estratos reales:", {k: len(v) for k, v in pools.items()})
    print("mediciones sintéticas:", len(meas))
    for name, reps in scen.items():
        cri = cri_by_cluster(reps)
        print(f"{name:8s} n={len(reps)}  CRI:", {k: round(v, 3) for k, v in cri.items()})
