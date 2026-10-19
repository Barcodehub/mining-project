"""
Prepara el conjunto real de la CDMB (datos.gov.co 9fpa-27t2) para reproducirlo por la API.

Entrada : evaluacion/real/raw/cdmb_9fpa-27t2_2026-10-19.json (sin modificar)
          límites municipales GADM 4.1 (nivel 2), ruta pasada por --gadm
Salidas : evaluacion/real/processed/cdmb_api.json      registros {sensorType, payload, meta}
          evaluacion/real/processed/cdmb_stations.csv  una fila por estación (coordenadas, municipio, categoría)
          evaluacion/real/processed/cdmb_transformations.json  registro de cada transformación aplicada

Transformaciones (todas documentadas en cdmb_transformations.json):
  T1. Coordenadas planas MAGNA-SIRGAS origen Bogotá (EPSG:3116) → WGS84 (EPSG:4326).
      Verificación: la quebrada La Baja cae en ~7.38 N, -72.90 W, junto a California (Santander).
  T2. Arsénico: la columna se rotula "mg/L", pero en la campaña oct–nov 2025 aparecen valores
      y límites de detección ≥ 1 (p. ej. "<5.00", "16.9"). Un LD de 5 mg/L sería 500 veces el
      límite de agua potable (0.01 mg/L), lo que es implausible para análisis rutinario; se
      interpretan como µg/L y se convierten a mg/L (÷1000). Los valores < 1 se dejan en mg/L.
  T3. El payload conserva los nombres de campo originales de la CDMB (fuente heterogénea); el
      adapter `cdmb_open_data` hace el mapeo y la conversión de Hg mg/L → µg/L (×1000).
  No se imputa nada: "-999" (no analizado) y "<LD" se envían tal como vienen.

Categoría de influencia minera por estación (criterio geográfico, no basado en los datos):
  DISTRITO_MINERO : estación en los municipios de California o Vetas (distrito minero
                    Vetas–California; Rincon-Vasquez et al., 2025; Espitia-Pérez et al., 2026)
  AGUAS_ABAJO     : río Vetas aguas abajo del distrito (salida, cf. VT18 en Rincon-Vasquez 2025)
                    y cauce principal del río Suratá, que recibe al Vetas
  OTRO            : sin influencia minera documentada en las fuentes revisadas
"""
import argparse
import csv
import json
import re
import sys
import unicodedata
from collections import Counter, OrderedDict
from pathlib import Path

import geopandas as gpd
from pyproj import Transformer
from shapely.geometry import Point

ROOT = Path(__file__).resolve().parents[3]
RAW = ROOT / "evaluacion/real/raw/cdmb_9fpa-27t2_2026-10-19.json"
OUT = ROOT / "evaluacion/real/processed"

MINING_MUNICIPALITIES = {"California", "Vetas"}
DOWNSTREAM_SOURCES = {"Río Suratá", "Río Vetas"}  # tramos fuera de los municipios mineros


def slug(s):
    s = unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode()
    return re.sub(r"[^A-Z0-9]+", "_", s.upper()).strip("_")


def as_mg_per_l(raw):
    """T2: normaliza el arsénico a mg/L conservando el prefijo '<'."""
    if raw is None or raw.strip() in ("", "-999"):
        return raw, False
    s = raw.strip()
    prefix = "<" if s.startswith("<") else ""
    num = s[1:].strip() if prefix else s
    try:
        v = float(num.replace(",", "."))
    except ValueError:
        return raw, False
    if v >= 1.0:
        return f"{prefix}{v / 1000.0:.6g}", True
    return raw, False


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--gadm", required=True, help="ruta a gadm41_COL_2.json")
    args = ap.parse_args()
    sys.stdout.reconfigure(encoding="utf-8")

    rows = json.loads(RAW.read_text(encoding="utf-8"))
    to_wgs84 = Transformer.from_crs("EPSG:3116", "EPSG:4326", always_xy=True)
    munis = gpd.read_file(args.gadm)[["NAME_1", "NAME_2", "geometry"]]

    OUT.mkdir(parents=True, exist_ok=True)
    records, transformations, stations = [], [], OrderedDict()
    as_converted = 0

    for i, r in enumerate(rows):
        lon, lat = to_wgs84.transform(float(r["este"]), float(r["norte"]))
        hit = munis[munis.contains(Point(lon, lat))]
        muni = hit.iloc[0]["NAME_2"] if len(hit) else None
        dept = hit.iloc[0]["NAME_1"] if len(hit) else None
        fuente = r["fuente"]
        if muni in MINING_MUNICIPALITIES:
            category = "DISTRITO_MINERO"
        elif fuente in DOWNSTREAM_SOURCES:
            category = "AGUAS_ABAJO"
        else:
            category = "OTRO"

        payload = dict(r)
        payload["lat"] = round(lat, 6)
        payload["lon"] = round(lon, 6)
        payload["cluster"] = slug(fuente)
        payload["c_digo_cdmb"] = re.sub(r"\s+", "", r["c_digo_cdmb"])
        new_as, changed = as_mg_per_l(r.get("arsenico_mg_l"))
        if changed:
            as_converted += 1
            transformations.append({"row": i, "field": "arsenico_mg_l", "rule": "T2",
                                    "from": r["arsenico_mg_l"], "to": new_as})
            payload["arsenico_mg_l"] = new_as

        records.append({
            "sensorType": "cdmb_open_data",
            "payload": payload,
            "meta": {"row": i, "fuente": fuente, "station": payload["c_digo_cdmb"],
                     "municipio": muni, "departamento": dept, "categoria": category,
                     "campana": r["fecha_y_hora_de_monitoreo"][:7],
                     "has_metals": r.get("mercurio_mg_l") not in (None, "", "-999")},
        })
        key = payload["c_digo_cdmb"]
        if key not in stations:
            stations[key] = {"station": key, "fuente": fuente, "cluster": payload["cluster"],
                             "lat": round(lat, 6), "lon": round(lon, 6), "altura_m": r.get("altura"),
                             "municipio": muni, "departamento": dept, "categoria": category, "n": 0}
        stations[key]["n"] += 1

    (OUT / "cdmb_api.json").write_text(json.dumps(records, ensure_ascii=False, indent=1), encoding="utf-8")
    with open(OUT / "cdmb_stations.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(next(iter(stations.values())).keys()))
        w.writeheader()
        w.writerows(stations.values())
    (OUT / "cdmb_transformations.json").write_text(json.dumps({
        "T1": "EPSG:3116 -> EPSG:4326 (pyproj)",
        "T2": "arsenico >= 1 interpretado como ug/L y convertido a mg/L",
        "T3": "adapter cdmb_open_data: mercurio_mg_l x1000 -> ug/L",
        "changes": transformations}, ensure_ascii=False, indent=1), encoding="utf-8")

    print(f"registros: {len(records)}  estaciones: {len(stations)}  As convertidos (T2): {as_converted}")
    print("sin municipio asignado:", sum(1 for s in stations.values() if s['municipio'] is None))
    print("categorías (registros):", Counter(r["meta"]["categoria"] for r in records))
    print("categorías (con metales):", Counter(r["meta"]["categoria"] for r in records if r["meta"]["has_metals"]))
    print("municipios:", Counter(s["municipio"] for s in stations.values()).most_common())


if __name__ == "__main__":
    main()
