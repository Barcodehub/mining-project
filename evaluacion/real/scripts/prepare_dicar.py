"""
Resume los operativos de la Policía Nacional (DICAR) contra la explotación ilícita de yacimientos
mineros en Santander y Norte de Santander, por municipio (datos.gov.co 4y5w-y5sj).

Entrada : evaluacion/real/raw/dicar_4y5w-y5sj_santanderes_2026-10-19.json (2.953 registros, jul-2024 a ago-2026)
Salida  : evaluacion/real/processed/dicar_municipios.csv

Columnas: registros totales, incautaciones de MERCURIO, registros de casos conocidos y minas intervenidas.
Nota: "MINAS INTERVENIDAS" incluye minería no metálica (arena, arcilla, carbón); por eso el indicador
específico de minería aurífera con amalgamación es el de incautación de mercurio.
"""
import csv
import json
import re
import sys
import unicodedata
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
RAW = ROOT / "evaluacion/real/raw/dicar_4y5w-y5sj_santanderes_2026-10-19.json"
OUT = ROOT / "evaluacion/real/processed/dicar_municipios.csv"


def norm_muni(s):
    """'Bucaramanga (CT)' → 'BUCARAMANGA'; quita tildes y espacios para casar con GADM ('ElPlayón')."""
    s = re.sub(r"\(.*?\)", "", s)
    s = unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode()
    return re.sub(r"[^A-Z]", "", s.upper())


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    rows = json.loads(RAW.read_text(encoding="utf-8"))
    acc = defaultdict(lambda: {"registros": 0, "mercurio": 0, "casos_conocidos": 0, "minas_intervenidas": 0})
    for r in rows:
        k = (r["departamento"], r["municipio"])
        a = acc[k]
        a["registros"] += 1
        if r["conducta"] == "MERCURIO":
            a["mercurio"] += 1
        elif r["conducta"] == "CASO CONOCIDO MINERIA ILEGAL":
            a["casos_conocidos"] += 1
        elif r["conducta"] == "MINAS INTERVENIDAS":
            a["minas_intervenidas"] += 1
    with open(OUT, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["departamento", "municipio", "municipio_norm", "registros", "mercurio",
                    "casos_conocidos", "minas_intervenidas"])
        for (dep, mun), a in sorted(acc.items(), key=lambda kv: -kv[1]["registros"]):
            w.writerow([dep, mun, norm_muni(mun), a["registros"], a["mercurio"], a["casos_conocidos"],
                        a["minas_intervenidas"]])
    print(f"municipios: {len(acc)} -> {OUT}")
    for (dep, mun), a in sorted(acc.items(), key=lambda kv: -kv[1]["mercurio"])[:8]:
        print(f"  {dep:20s} {mun:22s} mercurio={a['mercurio']:3d} registros={a['registros']}")


if __name__ == "__main__":
    main()
