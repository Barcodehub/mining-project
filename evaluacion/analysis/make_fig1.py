"""Figura 1 — Arquitectura del middleware v2 (generada por script). Uso: python evaluacion/analysis/make_fig1.py"""
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
plt.rcParams.update({"font.family": "serif", "font.serif": ["Times New Roman", "DejaVu Serif"],
                     "savefig.dpi": 300, "savefig.bbox": "tight"})
FS_T, FS_L, FS_A = 6.6, 5.7, 5.4
LH = 0.034  # interlineado


def box(ax, x, y, w, h, title, lines=(), fc="#ffffff", lw=0.8):
    ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.004,rounding_size=0.012",
                                fc=fc, ec="#333333", lw=lw))
    ax.text(x + w / 2, y + h - 0.016, title, ha="center", va="top", fontsize=FS_T, fontweight="bold")
    for i, t in enumerate(lines):
        ax.text(x + 0.010, y + h - 0.058 - i * LH, t, ha="left", va="top", fontsize=FS_L)


def arrow(ax, p, q, label=None, at=None, ls="-"):
    ax.add_patch(FancyArrowPatch(p, q, arrowstyle="-|>", mutation_scale=6, lw=0.6, color="#333333", ls=ls,
                                 shrinkA=0, shrinkB=0))
    if label:
        x, y = at if at else ((p[0] + q[0]) / 2, (p[1] + q[1]) / 2)
        ax.text(x, y, label, ha="center", va="center", fontsize=FS_A, style="italic",
                bbox=dict(boxstyle="square,pad=0.15", fc="white", ec="none"))


def main():
    fig, ax = plt.subplots(figsize=(7.16, 4.3))
    ax.set_xlim(-0.005, 1.012)
    ax.set_ylim(0, 1)
    ax.axis("off")

    # Columna 1: fuentes e identidad
    box(ax, 0.00, 0.63, 0.19, 0.33, "Evidence sources", [
        "• In-situ probes (IoT-ready)", "• Laboratory campaigns:", "   CDMB open data", "   (mg/L, '<LOD', −999)",
        "   CORPONOR reports", "   (nested JSON)", "• Citizen reports (app)"], fc="#f2f2f2")
    box(ax, 0.00, 0.33, 0.19, 0.22, "mining-users", ["• Identity & access", "• JWT issuance", "• Roles: citizen,",
                                                     "   validator, admin"], fc="#f2f2f2")

    # Columna 2: núcleo
    box(ax, 0.24, 0.30, 0.39, 0.66, "mining-management (hexagonal core)", [], lw=1.0)
    stages = [
        ("1  Source adapters", ["field mapping + unit factors (Hg mg/L ×1000)", "sentinel −999 → absent"], "#eaf2fb"),
        ("2  Data-quality gate", ["'<LOD' kept as measured (censored)", "plausibility ranges → quality flags"], "#eaf2fb"),
        ("3  Ingestion-time classification", ["11 factors: Colombian law + EPA/CCME", "composite + normative floor"], "#fff4e5"),
        ("4  Citizen reports & privacy", ["validation workflow; 0.01° grid,", "HMAC pseudonym, EXIF/GPS stripping"], "#eaf7ea"),
    ]
    y = 0.775
    for t, ls, fc in stages:
        box(ax, 0.255, y, 0.36, 0.125, t, ls, fc=fc)
        y -= 0.145
    for y0 in (0.775, 0.63, 0.485):
        arrow(ax, (0.435, y0), (0.435, y0 - 0.02))
    ax.text(0.435, 0.315, "policy: config-ambiental.yaml (editable without recompilation)", ha="center", va="center",
            fontsize=FS_A, style="italic")

    # Columna 3: bus y consumidor
    box(ax, 0.685, 0.40, 0.135, 0.46, "Apache Kafka", ["topics:", "• environmental-", "  alerts", "  (key = cluster;",
                                                        "  Alert/Critical)", "• mining-events", "  (reports, audit)"],
        fc="#fdecea")
    box(ax, 0.845, 0.50, 0.155, 0.42, "audit-notifications", ["• idempotent", "  consumer", "  (dedup by id)",
                                                             "• audit trail", "  (PostgreSQL)", "• notifications",
                                                             "  (MongoDB)", "• alert.e2e.latency:", "  ingestion →",
                                                             "  persisted notification"], fc="#ffffff")

    # Fila inferior
    box(ax, 0.24, 0.03, 0.22, 0.20, "PostgreSQL", ["measurements + scores", "reports, adapters", "SAI, CRI, Spearman ρ"],
        fc="#f2f2f2")
    box(ax, 0.62, 0.03, 0.38, 0.20, "Observability & evaluation", ["Micrometer → Prometheus (latency histograms)",
                                                                   "k6 load tests, 3 heterogeneous sources",
                                                                   "independent oracle + Python replica"], fc="#f2f2f2")

    # Flechas
    arrow(ax, (0.19, 0.80), (0.255, 0.80), "REST", at=(0.222, 0.83))
    arrow(ax, (0.19, 0.44), (0.24, 0.44), "JWT", at=(0.215, 0.47))
    arrow(ax, (0.35, 0.30), (0.35, 0.23), "persist", at=(0.30, 0.265))
    arrow(ax, (0.615, 0.55), (0.685, 0.55), "publish\n(async)", at=(0.658, 0.588))
    arrow(ax, (0.615, 0.405), (0.685, 0.405), "events", at=(0.658, 0.432))
    arrow(ax, (0.82, 0.71), (0.845, 0.71))
    arrow(ax, (0.9225, 0.50), (0.9225, 0.23), ls="--")
    arrow(ax, (0.46, 0.13), (0.62, 0.13), ls="--")

    out = ROOT / "articulo/figuras"
    out.mkdir(parents=True, exist_ok=True)
    fig.savefig(out / "F1_arquitectura.png")
    fig.savefig(out / "F1_arquitectura.pdf")


if __name__ == "__main__":
    main()
