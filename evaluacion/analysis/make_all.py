"""
Genera TODAS las tablas y figuras del manuscrito v2 a partir de los resultados crudos.

  python evaluacion/analysis/make_all.py [--tag 2026-10-19] [--mc 1000]

Salidas:
  articulo/tablas/*.md + *.csv
  articulo/figuras/*.png (300 dpi) + *.pdf
  articulo/tablas/resultados_clave.json (cifras citadas en el texto)
"""
import argparse
import csv
import itertools
import json
import math
import re
import statistics
import sys
from collections import Counter, defaultdict
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
import numpy as np  # noqa: E402
from scipy import stats  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
sys.path[:0] = [str(ROOT / "evaluacion/analysis"), str(ROOT / "evaluacion/synthetic")]
from scoring_py import classify, variant  # noqa: E402
from oracle import load_policy, check, expected_floor  # noqa: E402
import generator as gen  # noqa: E402
from real_canonical import real_canonical  # noqa: E402
from perf_summary import run_summary, buckets  # noqa: E402

TAB, FIG = ROOT / "articulo/tablas", ROOT / "articulo/figuras"
LEVELS = ["INDETERMINADO", "NORMAL", "PRECAUCION", "ALERTA", "CRITICO"]
LEVEL_EN = {"INDETERMINADO": "Indeterminate", "NORMAL": "Normal", "PRECAUCION": "Caution",
            "ALERTA": "Alert", "CRITICO": "Critical"}
LEVEL_COLORS = {"INDETERMINADO": "#bdbdbd", "NORMAL": "#4daf4a", "PRECAUCION": "#ffd92f",
                "ALERTA": "#ff7f00", "CRITICO": "#e41a1c"}
KEY = {}

plt.rcParams.update({"font.family": "serif", "font.serif": ["Times New Roman", "DejaVu Serif"], "font.size": 8,
                     "axes.titlesize": 8, "axes.labelsize": 8, "legend.fontsize": 7, "xtick.labelsize": 7,
                     "ytick.labelsize": 7, "figure.dpi": 150, "savefig.dpi": 300, "savefig.bbox": "tight"})
COL1, COL2 = 3.5, 7.16  # anchos IEEE (pulgadas)


def jl(path):
    return [json.loads(x) for x in open(path, encoding="utf-8")]


def save_table(name, header, rows, md_title):
    TAB.mkdir(parents=True, exist_ok=True)
    with open(TAB / f"{name}.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(header)
        w.writerows(rows)
    lines = [f"# {md_title}", "", "| " + " | ".join(header) + " |", "|" + "---|" * len(header)]
    lines += ["| " + " | ".join(str(c) for c in r) + " |" for r in rows]
    (TAB / f"{name}.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


def save_fig(fig, name):
    FIG.mkdir(parents=True, exist_ok=True)
    fig.savefig(FIG / f"{name}.png")
    fig.savefig(FIG / f"{name}.pdf")
    plt.close(fig)


def canon_from_body(b):
    m = {"ph": b.get("ph"), "temperature": b.get("temperature"), "turbidity": b.get("turbidity"),
         "dissolved_oxygen": b.get("dissolvedOxygen"), "redox": b.get("redox"), "hg_total": b.get("hgTotal"),
         "hg_dissolved": b.get("hgDissolved"), "cyanide": b.get("cyanide"), "arsenic": b.get("arsenic"),
         "hg_sediment": b.get("hgSediment"), "hg_fish": b.get("hgFish"), "methylmercury": b.get("methylmercury"),
         "altitude": b.get("altitude"), "thermal_profile": b.get("thermalProfile"), "fish_predatory": b.get("fishPredatory")}
    for c in (b.get("censoredFields") or "").split(","):
        if "<" in c:
            k, v = c.split("<")
            m[k] = "<" + v
    m["_implausible"] = {f.split(":")[1].split("=")[0] for f in (b.get("qualityFlags") or "").split(",")
                         if f.startswith("IMPLAUSIBLE:")}
    return m


# ── T1: umbrales (desde el YAML) ───────────────────────────────────────────
def t1_thresholds(pol):
    rows = []
    for f in pol["factores"]:
        alert = f.get("alerta")
        if alert is None:
            alert = f"{f.get('alerta-fria')} (fría) / {f.get('alerta-calida')} (cálida)"
        crit = f["critico"] if f.get("critico-predador") is None else f"{f['critico']} / {f['critico-predador']} (predadoras)"
        rows.append([f["id"], f["parametro"], f["dimension"], f["tipo"], "> " if f["direccion"] == "MAYOR" else "< ",
                     alert, crit])
    save_table("T1_umbrales_modelo", ["Factor", "Parámetro", "Dimensión", "Tipo", "Superación", "ALERTA", "CRÍTICO"],
               rows, "Tabla 1 — Factores y umbrales del modelo (fuentes en tabla_umbrales.md)")


# ── V1: datos reales, v1 vs v2 ─────────────────────────────────────────────
def v1_real(R, pol):
    raw = jl(R / "functional_v2/real_cdmb_raw.jsonl") + jl(R / "functional_v2/real_corponor_raw.jsonl")
    canon = real_canonical()
    cdmb_meta = [r["meta"] for r in json.loads((ROOT / "evaluacion/real/processed/cdmb_api.json").read_text(encoding="utf-8"))]
    v1_rows = {r["device_id"]: r for r in csv.DictReader(open(R / "functional_v1/v1_stored.csv", encoding="utf-8"))}
    v1_resp = jl(R / "functional_v1/real_canonical.jsonl")
    cat = []
    for i, c in enumerate(canon):
        cat.append(cdmb_meta[i]["categoria"] if i < len(cdmb_meta) else "CORPONOR")
    # niveles v2 por categoría (formato crudo)
    by = defaultdict(Counter)
    for i, r in enumerate(raw):
        by[cat[i]][r["body"]["alertLevel"]] += 1
    # v1
    v1c = Counter()
    for i, c in enumerate(canon):
        dev = c["payload"]["deviceId"]
        if dev in v1_rows:
            v1c[v1_rows[dev]["alert_level"]] += 1
        else:
            msg = v1_resp[i]["body"].get("message", "")
            v1c["RECHAZADA (Hg ausente o < LD)" if "hg_" in msg else "RECHAZADA (campos de campo faltantes)"] += 1
    # superaciones legales reales y detección
    legal = Counter()
    for i, c in enumerate(canon):
        m = {k: c["payload"].get(k) for k in ("ph", "temperature", "turbidity", "dissolved_oxygen", "hg_total",
                                               "cyanide", "arsenic", "altitude")}
        floor, ev = expected_floor(m, pol)
        human = [k for k, s in ev.items() if s in ("ALERTA", "CRITICO") and k.endswith("humano")]
        if human:
            legal["con superación humana"] += 1
            lv2 = raw[i]["body"]["alertLevel"]
            legal["v2 ≥ PRECAUCION"] += lv2 in ("PRECAUCION", "ALERTA", "CRITICO")
            dev = c["payload"]["deviceId"]
            lv1 = v1_rows[dev]["alert_level"] if dev in v1_rows else "RECHAZADA"
            legal["v1 ≥ PRECAUCION"] += lv1 in ("PRECAUCION", "ALERTA", "CRITICO")
            legal["v1 rechazada"] += lv1 == "RECHAZADA"
    rows = []
    for k in ("DISTRITO_MINERO", "AGUAS_ABAJO", "OTRO", "CORPONOR"):
        c = by[k]
        rows.append([k, sum(c.values())] + [c.get(lv, 0) for lv in LEVELS])
    tot = Counter()
    for c in by.values():
        tot.update(c)
    rows.append(["TOTAL v2", sum(tot.values())] + [tot.get(lv, 0) for lv in LEVELS])
    save_table("T2_v1_reales_por_categoria", ["Categoría", "n"] + LEVELS, rows,
               "Tabla 2 — V1: 345 mediciones reales (CDMB 2024–2025 + CORPONOR 2025) clasificadas por el v2")
    rows2 = [[k, v] for k, v in sorted(v1c.items())]
    save_table("T2b_v1_sistema_v1", ["Resultado en el sistema v1", "n"], rows2,
               "Tabla 2b — Mismas 345 mediciones en el sistema v1 (etiqueta v1-paper)")
    KEY["v1_real"] = {"v2_levels": dict(tot), "v1_outcomes": dict(v1c), "legal_human": dict(legal)}

    # CN en distrito/aguas abajo vs resto (independiente del modelo): Fisher
    cn_hi = cn_lo = n_hi = n_lo = 0
    for i, c in enumerate(canon[:len(cdmb_meta)]):
        if not cdmb_meta[i]["has_metals"]:
            continue
        v = c["payload"].get("cyanide")
        exceeds = isinstance(v, float) and v > 0.05
        if cat[i] in ("DISTRITO_MINERO", "AGUAS_ABAJO"):
            n_hi += 1
            cn_hi += exceeds
        else:
            n_lo += 1
            cn_lo += exceeds
    odds, p = stats.fisher_exact([[cn_hi, n_hi - cn_hi], [cn_lo, n_lo - cn_lo]])
    KEY["cn_fisher"] = {"mining_exceed": cn_hi, "mining_n": n_hi, "other_exceed": cn_lo, "other_n": n_lo,
                        "odds_ratio": odds, "p": p}
    return raw, canon, cat


def fig_map(raw, canon, cat):
    import geopandas as gpd
    # Límites GADM 4.1 (nivel 2). No se redistribuyen (licencia GADM): se descargan de
    # https://geodata.ucdavis.edu/gadm/gadm4.1/json/gadm41_COL_2.json.zip y se pasan con --gadm.
    g = gpd.read_file(MAP_GADM)
    g = g[g["NAME_1"].isin(["Santander", "NortedeSantander", "Norte de Santander"])]
    fig, ax = plt.subplots(figsize=(COL1, 4.0))
    g.plot(ax=ax, color="#f5f5f5", edgecolor="#9e9e9e", linewidth=0.3)
    g[g["NAME_2"].isin(["California", "Vetas"])].plot(ax=ax, color="#fde0dd", edgecolor="#c51b8a", linewidth=0.6)
    # Peor nivel por estación (código de estación); posición = primera coordenada reportada
    worst, pos = {}, {}
    for i, r in enumerate(raw):
        b = r["body"]
        key = b["deviceId"]
        pos.setdefault(key, (b["latitude"], b["longitude"]))
        lv = b["alertLevel"]
        if key not in worst or LEVELS.index(lv) > LEVELS.index(worst[key]):
            worst[key] = lv
    for lv in LEVELS:
        pts = [pos[k] for k, v in worst.items() if v == lv]
        if pts:
            ax.scatter([p[1] for p in pts], [p[0] for p in pts], s=10 if lv in ("NORMAL", "INDETERMINADO") else 16,
                       c=LEVEL_COLORS[lv], edgecolors="k", linewidths=0.3, label=f"{LEVEL_EN[lv]} ({len(pts)})", zorder=3)
    ax.set_xlim(-73.45, -72.2)
    ax.set_ylim(6.95, 8.15)
    ax.set_xlabel("Longitude (°)")
    ax.set_ylabel("Latitude (°)")
    ax.legend(loc="upper center", bbox_to_anchor=(0.5, -0.13), ncol=4, frameon=False, columnspacing=0.8,
              handletextpad=0.2, title="Worst alert level per station (CDMB and CORPONOR, 2025)",
              title_fontsize=7)
    ax.text(-72.86, 7.43, "California–Vetas\nmining district", fontsize=6, color="#c51b8a")
    save_fig(fig, "F2_mapa_estaciones")


# ── V2: inyección v1 vs v2 frente al oráculo ───────────────────────────────
def v2_injection(R, pol):
    inj = json.loads((ROOT / "evaluacion/data/v2/inyeccion_eventos.json").read_text(encoding="utf-8"))
    v2 = jl(R / "functional_v2/injection.jsonl")
    v1 = {r["device_id"]: r for r in csv.DictReader(open(R / "functional_v1/v1_stored.csv", encoding="utf-8"))}
    res = {"v2": defaultdict(Counter), "v1": defaultdict(Counter)}
    for r in v2:
        rec = inj[r["i"]]
        m = {k: rec["payload"].get(k) for k in ("ph", "temperature", "turbidity", "dissolved_oxygen", "hg_total",
                                                 "cyanide", "arsenic", "altitude")}
        t = rec["meta"]["target"]
        res["v2"][t][check(r["body"]["alertLevel"], m, pol)] += 1
        s = v1.get(rec["payload"]["deviceId"])
        res["v1"][t]["REJECTED" if s is None else check(s["alert_level"], m, pol)] += 1
    outcomes = ["OK", "MASKED", "FLOOR_VIOLATION", "FALSE_ALARM", "REJECTED"]
    rows = []
    for sysn in ("v1", "v2"):
        tot = Counter()
        for t in ("hg_total", "cyanide", "arsenic", "ph", "dissolved_oxygen"):
            c = res[sysn][t]
            tot.update(c)
            rows.append([sysn, t, sum(c.values())] + [c.get(o, 0) for o in outcomes])
        rows.append([sysn, "TOTAL", sum(tot.values())] + [tot.get(o, 0) for o in outcomes])
        KEY[f"injection_{sysn}"] = dict(tot)
    save_table("T3_v2_inyeccion", ["Sistema", "Parámetro", "n"] + outcomes, rows,
               "Tabla 3 — V2: 3.564 variantes inyectadas sobre 99 observaciones reales, frente al oráculo normativo")
    fig, axes = plt.subplots(1, 2, figsize=(COL2, 2.2), sharey=True)
    colors = {"OK": "#1b9e77", "MASKED": "#d95f02", "FLOOR_VIOLATION": "#e6ab02", "FALSE_ALARM": "#7570b3",
              "REJECTED": "#999999"}
    targets = ["hg_total", "cyanide", "arsenic", "ph", "dissolved_oxygen"]
    labels = ["Hg", "CN", "As", "pH", "DO"]
    for ax, sysn in zip(axes, ("v1", "v2")):
        left = np.zeros(len(targets))
        for o in outcomes:
            vals = np.array([res[sysn][t].get(o, 0) / max(1, sum(res[sysn][t].values())) for t in targets]) * 100
            ax.barh(labels, vals, left=left, color=colors[o], label={"OK": "Correct", "MASKED": "Masked", "FLOOR_VIOLATION": "Floor violation", "FALSE_ALARM": "False alarm", "REJECTED": "Rejected"}[o])
            left += vals
        ax.set_title(f"System {sysn}")
        ax.set_xlim(0, 100)
        ax.set_xlabel("% of injected variants")
    axes[1].legend(loc="center left", bbox_to_anchor=(1.01, 0.5), frameon=False)
    save_fig(fig, "F3_inyeccion_v1_v2")


# ── H3: fidelidad de la normalización heterogénea ──────────────────────────
def h3_fidelity(raw, canon):
    fields = {"ph": "ph", "temperature": "temperature", "turbidity": "turbidity",
              "dissolved_oxygen": "dissolvedOxygen", "hg_total": "hgTotal", "cyanide": "cyanide", "arsenic": "arsenic"}
    c = Counter()
    for r, e in zip(raw, canon):
        b = r["body"]
        cens = dict(x.split("<") for x in (b.get("censoredFields") or "").split(",") if "<" in x)
        implaus = {f.split(":")[1].split("=")[0] for f in (b.get("qualityFlags") or "").split(",") if f.startswith("IMPLAUSIBLE:")}
        for p, bf in fields.items():
            exp = e["payload"].get(p)
            got = b.get(bf)
            if exp is None:
                c["ausente_ok" if got is None else "error"] += 1
            elif isinstance(exp, str) and exp.startswith("<"):
                ok = got is None and p in cens and math.isclose(float(cens[p]), float(exp[1:]), rel_tol=1e-9)
                c["censurado_ok" if ok else "error"] += 1
            elif p in implaus:
                c["implausible_marcado" if got is None else "error"] += 1
            else:
                c["valor_ok" if got is not None and math.isclose(got, exp, rel_tol=1e-9, abs_tol=1e-12) else "error"] += 1
    rows = [[k, v] for k, v in sorted(c.items())]
    save_table("T4_h3_fidelidad", ["Resultado por campo", "n"], rows,
               "Tabla 4 — H3: fidelidad campo a campo de 345 filas reales en formato original (CDMB y CORPONOR) vs. valor canónico esperado")
    KEY["h3"] = dict(c)


# ── V3: Monte Carlo SAI/CRI ────────────────────────────────────────────────
def v3_montecarlo(pol, n_rep, cache=None):
    pools = gen.load_pools()
    perms = np.array(list(itertools.permutations(range(7))))

    def ranks(x):
        return stats.rankdata(x)

    def rho_p(sai, cri):
        rx, ry = ranks(sai), ranks(cri)
        if np.std(rx) == 0 or np.std(ry) == 0:
            return np.nan, np.nan
        rxc = (rx - rx.mean()) / np.sqrt(((rx - rx.mean()) ** 2).sum())
        ryc = (ry - ry.mean()) / np.sqrt(((ry - ry.mean()) ** 2).sum())
        r = float((rxc * ryc).sum())
        rp = (rxc[None, :] * ryc[perms]).sum(axis=1)
        return r, float((np.abs(rp) >= abs(r) - 1e-12).mean())

    scenarios = [("aligned", 0.0), ("null", 0.0), ("noisy", 0.1), ("noisy", 0.2), ("noisy", 0.3), ("noisy", 0.5)]
    out = {f"{s}{int(n * 100) if s == 'noisy' else ''}": {"rho": [], "p": []} for s, n in scenarios}
    cids = [c[0] for c in gen.CLUSTERS]
    if cache and cache.exists():
        out = json.loads(cache.read_text(encoding="utf-8"))
    reps_to_run = 0 if (cache and cache.exists()) else n_rep
    for rep in range(reps_to_run):
        seed = 100000 + rep
        meas = gen.make_measurements(pools, seed=seed)
        alerts, totals = Counter(), Counter()
        for r in meas:
            lv = classify(r["payload"], pol)["alertLevel"]
            if lv == "INDETERMINADO":
                continue
            totals[r["payload"]["clusterId"]] += 1
            alerts[r["payload"]["clusterId"]] += lv in ("ALERTA", "CRITICO")
        sai = np.array([alerts[c] / totals[c] if totals[c] else 0 for c in cids])
        for s, n in scenarios:
            cri = gen.cri_by_cluster(gen.make_reports(s, noise=n, seed=seed))
            rr, pp = rho_p(sai, np.array([cri[c] for c in cids]))
            k = f"{s}{int(n * 100) if s == 'noisy' else ''}"
            out[k]["rho"].append(rr)
            out[k]["p"].append(pp)
        if (rep + 1) % 100 == 0:
            print(f"    MC {rep + 1}/{n_rep}", flush=True)
    if cache and not cache.exists():
        cache.write_text(json.dumps(out), encoding="utf-8")
    rows = []
    for k, v in out.items():
        rho = np.array(v["rho"], dtype=float)
        p = np.array(v["p"], dtype=float)
        ok = ~np.isnan(rho)
        lo, hi = np.percentile(rho[ok], [2.5, 97.5])
        rows.append([k, int(ok.sum()), f"{np.mean(rho[ok]):.3f}", f"{np.median(rho[ok]):.3f}", f"[{lo:.3f}, {hi:.3f}]",
                     f"{np.mean(p[ok] < 0.05):.3f}"])
        KEY.setdefault("v3_mc", {})[k] = {"mean_rho": float(np.mean(rho[ok])), "ci95": [float(lo), float(hi)],
                                         "rate_p_lt_0_05": float(np.mean(p[ok] < 0.05)), "n": int(ok.sum())}
    save_table("T5_v3_montecarlo", ["Escenario", "réplicas", "ρ medio", "ρ mediana", "IC 95 % de ρ",
                                   "Proporción p < 0.05 (potencia; FPR en null)"], rows,
               f"Tabla 5 — V3: Monte Carlo ({n_rep} réplicas) de la co-localización SAI–CRI con verdad de terreno conocida")
    fig, ax = plt.subplots(figsize=(COL1, 2.3))
    keys = list(out.keys())
    data = [np.array(out[k]["rho"], dtype=float)[~np.isnan(np.array(out[k]["rho"], dtype=float))] for k in keys]
    ax.boxplot(data, tick_labels=["Aligned", "Null", "Noise\n10%", "Noise\n20%", "Noise\n30%", "Noise\n50%"],
               showfliers=False, widths=0.6, medianprops={"color": "k", "linewidth": 1.2})
    ax.axhline(0, color="k", lw=0.5, ls=":")
    ax.set_ylabel("Spearman ρ (SAI vs CRI)")
    ax.set_ylim(-1.05, 1.05)
    save_fig(fig, "F5_v3_montecarlo")


# ── Rendimiento ────────────────────────────────────────────────────────────
def perf(R):
    base = R / "perf"
    runs = {d.name: run_summary(d) for d in sorted(base.glob("*_r*")) if (d / "run.json").exists()}
    rows = []
    agg = {}
    for scen, vu in (("bajo", 50), ("medio", 200), ("alto", 500)):
        rs = [v for k, v in runs.items() if k.startswith(scen + "_")]
        a = {k: (statistics.mean([r[k] for r in rs]), statistics.stdev([r[k] for r in rs])) for k in rs[0]
             if all(isinstance(r[k], (int, float)) for r in rs)}
        agg[scen] = a

        def f(k, d=2):
            return f"{a[k][0]:.{d}f} ± {a[k][1]:.{d}f}"
        rows.append([f"{scen} ({vu} VU)", len(rs), f("iterations", 0), f("req_per_s", 1), f"{a['http_failed_rate'][0] * 100:.2f}",
                     f"{a['checks_rate'][0] * 100:.2f}", f("post_med_p50"), f("post_med_p95"), f("post_med_p99"),
                     f("e2e_p50_ms"), f("e2e_p95_ms"), f("e2e_p99_ms"), f("alerts_e2e_count", 0), f("alerts_per_s", 2),
                     int(a["alerts_publish_failed"][0])])
    save_table("T6_rendimiento", ["Escenario", "rep.", "Iteraciones", "req/s", "Error HTTP (%)", "Checks (%)",
                                  "POST p50 (ms)", "POST p95 (ms)", "POST p99 (ms)", "E2E p50 (ms)", "E2E p95 (ms)",
                                  "E2E p99 (ms)", "Alertas entregadas", "Alertas/s", "Publicaciones fallidas"], rows,
               "Tabla 6 — Rendimiento (media ± DE de 3 repeticiones independientes en entorno limpio)")
    KEY["perf"] = {s: {k: v[0] for k, v in a.items()} for s, a in agg.items()}
    # CDF de latencia E2E (histogramas agregados de las 3 repeticiones)
    fig, ax = plt.subplots(figsize=(COL1, 2.2))
    for scen, ls, vu in (("bajo", ":", 50), ("medio", "--", 200), ("alto", "-", 500)):
        tot = Counter()
        for d in sorted(base.glob(f"{scen}_r*")):
            bb = buckets((d / "audit_before.txt").read_text(encoding="utf-8"), "alert_e2e_latency_seconds")
            ba = buckets((d / "audit_after.txt").read_text(encoding="utf-8"), "alert_e2e_latency_seconds")
            for le in ba:
                tot[le] += ba[le] - bb.get(le, 0)
        les = sorted(k for k in tot if k != float("inf"))
        n = tot[float("inf")]
        ax.plot([le * 1000 for le in les], [tot[le] / n for le in les], ls=ls, color="k",
                label=f"{vu} VU ({int(n)} alerts, 3 runs)")
    ax.set_xscale("log")
    ax.set_xlim(2, 200)
    ax.set_xlabel("End-to-end alert latency (ms): ingestion → persisted notification")
    ax.set_ylabel("Cumulative fraction")
    ax.legend(frameon=False, loc="lower right")
    ax.grid(alpha=0.3, which="both", lw=0.3)
    save_fig(fig, "F4_latencia_e2e_cdf")
    # recursos (docker stats) en alto
    res = defaultdict(lambda: {"cpu": [], "mem": []})
    for d in sorted(base.glob("alto_r*")):
        for row in csv.DictReader(open(d / "docker_stats.csv", encoding="utf-8")):
            name = re.sub(r"^exp-perf-|-\d+$", "", row["container"])
            name = "k6 (generador de carga)" if name.startswith("k6-run") else name
            try:
                res[name]["cpu"].append(float(row["cpu_perc"].rstrip("%")))
                mu = row["mem_usage"].split("/")[0].strip()
                val = float(re.findall(r"[\d.]+", mu)[0])
                res[name]["mem"].append(val / 1024 if mu.endswith("KiB") else val * 1024 if mu.endswith("GiB") else val)
            except (ValueError, IndexError):
                pass
    rows = [[k, f"{np.mean(v['cpu']):.1f}", f"{np.percentile(v['cpu'], 95):.1f}", f"{max(v['mem']):.0f}"]
            for k, v in sorted(res.items()) if v["cpu"]]
    save_table("T7_recursos_alto", ["Contenedor", "CPU media (%)", "CPU p95 (%)", "Memoria máx. (MiB)"], rows,
               "Tabla 7 — Recursos por contenedor durante el escenario alto (3 repeticiones; 100 % = 1 vCPU)")


# ── Sensibilidad de la política ────────────────────────────────────────────
def sensitivity(raw, pol0):
    base_pol = variant(pol0)
    ms = [canon_from_body(r["body"]) for r in raw]
    for m, r in zip(ms, raw):  # re-derivar perfil por altitud en cada variante
        m.pop("thermal_profile", None)
    base = [classify(m, base_pol)["alertLevel"] for m in ms]
    variants = [
        ("Pesos iguales (1/3, 1/3, 1/3)", variant(pol0, pesos=(1 / 3, 1 / 3, 1 / 3)), "humano"),
        ("Pesos humano-dominante (0.60, 0.25, 0.15)", variant(pol0, pesos=(0.60, 0.25, 0.15)), "humano"),
        ("Pesos ecológico-dominante (0.25, 0.50, 0.25)", variant(pol0, pesos=(0.25, 0.50, 0.25)), "humano"),
        ("Cortes −0.05 (0.20/0.45/0.70)", variant(pol0, cortes=(0.20, 0.45, 0.70)), "humano"),
        ("Cortes +0.05 (0.30/0.55/0.80)", variant(pol0, cortes=(0.30, 0.55, 0.80)), "humano"),
        ("Perfil frío desde 1601 m (−400 m)", variant(pol0, altitud_fria=1601), "humano"),
        ("Perfil frío desde 2401 m (+400 m)", variant(pol0, altitud_fria=2401), "humano"),
        ("Piso: cualquier CRÍTICO de norma colombiana → ALERTA", variant(pol0), "normativo_co"),
        ("Piso: cualquier superación CRÍTICA (cualquier dimensión) → ALERTA", variant(pol0), "cualquier_critico"),
        ("Sin piso normativo", variant(pol0), "ninguno"),
    ]
    rows = []
    for name, pol, rule in variants:
        lv = [classify(m, pol, floor_rule=rule)["alertLevel"] for m in ms]
        changed = sum(a != b for a, b in zip(base, lv))
        up = sum(LEVELS.index(b) > LEVELS.index(a) for a, b in zip(base, lv))
        alert_base = sum(x in ("ALERTA", "CRITICO") for x in base)
        alert_v = sum(x in ("ALERTA", "CRITICO") for x in lv)
        cats = sorted(set(base) | set(lv), key=LEVELS.index)
        kappa = cohen_kappa(base, lv, cats)
        masked = sum(1 for m, x in zip(ms, lv) if x in ("NORMAL", "INDETERMINADO") and
                     any(e.endswith("humano:ALERTA") or e.endswith("humano:CRITICO") or e.startswith("hg-pescado")
                         for e in classify(m, base_pol)["exceedances"]))
        rows.append([name, f"{changed} ({100 * changed / len(ms):.1f} %)", up, changed - up, f"{alert_base} → {alert_v}",
                     f"{kappa:.3f}", masked])
    save_table("T8_sensibilidad", ["Variante de política", "Mediciones que cambian de nivel", "Suben", "Bajan",
                                   "ALERTA+CRÍTICO (base → variante)", "κ de Cohen vs. base",
                                   "Superaciones humanas enmascaradas"], rows,
               "Tabla 8 — Sensibilidad de la clasificación de las 345 mediciones reales a la política")
    KEY["sensitivity"] = rows


def cohen_kappa(a, b, cats):
    n = len(a)
    po = sum(x == y for x, y in zip(a, b)) / n
    ca, cb = Counter(a), Counter(b)
    pe = sum(ca[c] * cb[c] for c in cats) / n ** 2
    return 1.0 if pe == 1 else (po - pe) / (1 - pe)


# ── Contraste externo con los operativos de la Policía ─────────────────────
def external(raw, canon, cat, pol):
    import unicodedata

    def norm(s):
        s = re.sub(r"\(.*?\)", "", s or "")
        s = unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode()
        return re.sub(r"[^A-Z]", "", s.upper())
    st = {r["station"]: r for r in csv.DictReader(open(ROOT / "evaluacion/real/processed/cdmb_stations.csv", encoding="utf-8"))}
    dicar = {r["municipio_norm"]: r for r in csv.DictReader(open(ROOT / "evaluacion/real/processed/dicar_municipios.csv", encoding="utf-8"))
             if r["departamento"] == "SANTANDER"}
    meta = [r["meta"] for r in json.loads((ROOT / "evaluacion/real/processed/cdmb_api.json").read_text(encoding="utf-8"))]
    agg = defaultdict(lambda: [0, 0, 0])  # n con metales, n con superación humana, n ALERTA+
    for i, m in enumerate(meta):
        if not m["has_metals"]:
            continue
        mun = norm(st[m["station"]]["municipio"])
        p = canon[i]["payload"]
        _, ev = expected_floor({k: p.get(k) for k in ("ph", "temperature", "turbidity", "dissolved_oxygen",
                                                       "hg_total", "cyanide", "arsenic", "altitude")}, pol)
        a = agg[mun]
        a[0] += 1
        a[1] += any(k.endswith("humano") and s in ("ALERTA", "CRITICO") for k, s in ev.items())
        a[2] += raw[i]["body"]["alertLevel"] in ("ALERTA", "CRITICO")
    rows, x, y = [], [], []
    for mun, (n, h, al) in sorted(agg.items(), key=lambda kv: -kv[1][1] / kv[1][0]):
        d = dicar.get(mun, {"mercurio": 0, "registros": 0})
        rows.append([mun, n, h, f"{h / n:.2f}", al, int(d["mercurio"]), int(d["registros"])])
        x.append(h / n)
        y.append(int(d["mercurio"]))
    rho, p = stats.spearmanr(x, y)
    # p por permutación (Monte Carlo, semilla fija)
    rng = np.random.default_rng(20261019)
    yy = np.array(y)
    perm = np.array([stats.spearmanr(x, rng.permutation(yy))[0] for _ in range(20000)])
    pperm = float((np.abs(perm) >= abs(rho) - 1e-12).mean())
    save_table("T9_contraste_dicar", ["Municipio (CDMB)", "Muestras con metales", "Con superación humana",
                                      "Proporción", "ALERTA+CRÍTICO (v2)", "Incautaciones de Hg (Policía)",
                                      "Registros DICAR"], rows,
               f"Tabla 9 — Contraste externo: superaciones por municipio vs. incautaciones de mercurio (ρ = {rho:.3f}, p perm = {pperm:.3f}, n = {len(x)})")
    KEY["external"] = {"rho": rho, "p_perm": pperm, "n": len(x)}


MAP_GADM = None


def main():
    global MAP_GADM
    sys.stdout.reconfigure(encoding="utf-8")
    ap = argparse.ArgumentParser()
    ap.add_argument("--tag", default="2026-10-19")
    ap.add_argument("--mc", type=int, default=1000)
    ap.add_argument("--gadm", required=True)
    a = ap.parse_args()
    MAP_GADM = a.gadm
    R = ROOT / "evaluacion/results" / a.tag
    pol = load_policy()
    print("T1 umbrales"); t1_thresholds(pol)
    print("V1 reales"); raw, canon, cat = v1_real(R, pol)
    print("F2 mapa"); fig_map(raw, canon, cat)
    print("V2 inyección"); v2_injection(R, pol)
    print("H3 fidelidad"); h3_fidelity(raw, canon)
    print("Rendimiento"); perf(R)
    print("Sensibilidad"); sensitivity(raw, pol)
    print("Contraste externo"); external(raw, canon, cat, pol)
    print(f"V3 Monte Carlo ({a.mc})"); v3_montecarlo(variant(pol), a.mc, R / f"v3_montecarlo_{a.mc}.json")
    (TAB / "resultados_clave.json").write_text(json.dumps(KEY, ensure_ascii=False, indent=1, default=str), encoding="utf-8")
    print("listo")


if __name__ == "__main__":
    main()
