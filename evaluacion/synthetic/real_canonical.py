"""
Versión canónica (formato IoT, unidades del modelo) de las 345 filas reales (CDMB + CORPONOR).
Sirve para comparar v1 y v2 con exactamente la misma entrada: el v1 no soporta adapters con
conversión de unidades, así que a ambos se les entrega el Hg ya en µg/L.
"""
import json
from pathlib import Path

from generator import _scale

ROOT = Path(__file__).resolve().parents[2]
REAL = ROOT / "evaluacion/real/processed"


def real_canonical():
    out = []
    for r in json.loads((REAL / "cdmb_api.json").read_text(encoding="utf-8")):
        p = r["payload"]
        m = {"ph": _scale(p.get("ph_unidades_de_ph"), 1), "temperature": _scale(p.get("t_muestra_c"), 1),
             "turbidity": _scale(p.get("turbiedad_ntu"), 1),
             "dissolved_oxygen": _scale(p.get("oxigeno_disuelto_mg_l"), 1),
             "hg_total": _scale(p.get("mercurio_mg_l"), 1000), "cyanide": _scale(p.get("cianuro_mg_l"), 1),
             "arsenic": _scale(p.get("arsenico_mg_l"), 1), "altitude": _scale(p.get("altura"), 1)}
        ts = p["fecha_y_hora_de_monitoreo"].replace(".000", "") + "Z"
        out.append(({"latitude": p["lat"], "longitude": p["lon"], "clusterId": p["cluster"], "timestamp": ts, **m},
                    {"source": "CDMB", **r["meta"]}))
    for r in json.loads((REAL / "corponor_api.json").read_text(encoding="utf-8")):
        p = r["payload"]
        m = {"ph": _scale(p["campo"]["ph"], 1), "temperature": _scale(p["campo"]["temperatura_c"], 1),
             "turbidity": _scale(p["campo"]["turbiedad_ntu"], 1),
             "dissolved_oxygen": _scale(p["campo"]["oxigeno_mg_l"], 1),
             "hg_total": _scale(p["laboratorio"]["hg_total_mg_l"], 1000),
             "arsenic": _scale(p["laboratorio"]["as_total_mg_l"], 1), "altitude": p["punto"]["msnm"]}
        out.append(({"latitude": p["punto"]["lat"], "longitude": p["punto"]["lon"], "clusterId": p["cuenca"],
                     "timestamp": p["fecha"], **m}, {"source": "CORPONOR", **r["meta"]}))
    recs = []
    for i, (payload, meta) in enumerate(out):
        payload = {k: v for k, v in payload.items() if v is not None}
        payload["deviceId"] = f"CMP-{i:03d}"
        recs.append({"sensorType": "multi_parameter_sensor", "payload": payload, "meta": meta})
    return recs
