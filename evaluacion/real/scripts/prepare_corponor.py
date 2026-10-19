"""
Convierte la transcripción de CORPONOR 2025 (río Zulia IR-25-08; río Pamplonita IR-25-01) en
registros para la API, con un formato de campos distinto al de la CDMB (segunda fuente heterogénea).

Entrada : evaluacion/real/processed/corponor_2025_transcribed.csv
          (transcrito a mano desde las tablas de metales y de fisicoquímica de los PDF en
          evaluacion/real/raw; verificado contra `pdftotext -table`)
Salida  : evaluacion/real/processed/corponor_api.json

Transformaciones:
  C1. Coordenadas "MAGNA-SIRGAS Origen Nacional" (CTM12, EPSG:9377) → WGS84.
  C2. Fecha: los PDF no traen la fecha de muestreo por estación, solo la del informe de
      laboratorio. Se usa esa fecha y se marca con meta.fecha_aproximada = true.
  El adapter `corponor_report` convierte el Hg de mg/L a µg/L (×1000).
"""
import csv
import json
import re
import sys
import unicodedata
from pathlib import Path

from pyproj import Transformer

ROOT = Path(__file__).resolve().parents[3]
SRC = ROOT / "evaluacion/real/processed/corponor_2025_transcribed.csv"
OUT = ROOT / "evaluacion/real/processed/corponor_api.json"
REPORT_DATE = {"IR-25-08 (10-dic-2025)": "2025-12-10T12:00:00Z",
               "IR-25-01 (17-sep-2025)": "2025-09-17T12:00:00Z"}


def slug(s):
    s = unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode()
    return re.sub(r"[^A-Z0-9]+", "_", s.upper()).strip("_")


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    t = Transformer.from_crs("EPSG:9377", "EPSG:4326", always_xy=True)
    out = []
    with open(SRC, encoding="utf-8") as f:
        for r in csv.DictReader(f):
            lon, lat = t.transform(float(r["x_9377"]), float(r["y_9377"]))
            station = f"CORPONOR-{slug(r['cuenca'])}-ID{r['id']}"
            payload = {
                "estacion": station,
                "cuenca": slug(r["cuenca"]),
                "punto": {"lat": round(lat, 6), "lon": round(lon, 6), "msnm": float(r["altura_m"])},
                "fecha": REPORT_DATE[r["informe_lab"]],
                "campo": {"temperatura_c": r["temp_c"], "ph": r["ph"],
                          "oxigeno_mg_l": r["od_mg_l"], "turbiedad_ntu": r["turbiedad_ntu"]},
                "laboratorio": {"hg_total_mg_l": r["mercurio_mg_l"], "as_total_mg_l": r["arsenico_mg_l"]},
            }
            out.append({"sensorType": "corponor_report", "payload": payload,
                        "meta": {"documento": r["documento"], "informe": r["informe_lab"], "id": r["id"],
                                 "nombre": r["nombre"], "cuenca": r["cuenca"], "categoria": "OTRO",
                                 "fecha_aproximada": True}})
            print(f"{station:32s} {lat:8.4f} {lon:9.4f} {r['altura_m']:>5} m  Hg={r['mercurio_mg_l']:>8} As={r['arsenico_mg_l']}")
    OUT.write_text(json.dumps(out, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"registros: {len(out)} -> {OUT}")


if __name__ == "__main__":
    main()
