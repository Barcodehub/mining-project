"""
V2 — Inyección de eventos de magnitud conocida sobre la línea base real.

Para cada observación real con metales (99: CDMB + CORPONOR) y cada parámetro objetivo, se crea una
variante en la que solo ese parámetro toma un valor definido respecto de su umbral (por debajo,
justo por encima, por encima del crítico, …). El resto de la observación se conserva tal como se
midió (incluidos los "<LD"). La verdad esperada la calcula oracle.py, independiente del código Java.

Salida: evaluacion/data/v2/inyeccion_eventos.json
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generator import load_pools  # noqa: E402
from oracle import expected_floor, load_policy  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "evaluacion/data/v2/inyeccion_eventos.json"

# parámetro → valores inyectados (unidades canónicas), alrededor de los umbrales de la Tabla 1
TARGETS = {
    "hg_total": [0.5, 0.9, 1.1, 1.9, 2.1, 5.0, 20.0, 163.0],        # µg/L; 20 y 163 = valores de estrés sintéticos (no documentados)
    "cyanide": [0.03, 0.045, 0.055, 0.19, 0.21, 1.0, 3.14],          # mg/L; 3.14 = máximo real CDMB (La Baja)
    "arsenic": [0.005, 0.009, 0.011, 0.049, 0.051, 0.2],             # mg/L
    "ph": [7.0, 6.6, 6.4, 5.1, 4.9, 3.5, 2.78],                      # 2.78 = agua de mina en La Baja (Rincon-Vasquez 2025)
    "dissolved_oxygen": [6.0, 5.1, 4.9, 4.1, 3.9, 2.1, 1.9, 0.0],    # mg/L; 0.0 = Q. Tonchalá (CORPONOR 2025)
}


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    pol = load_policy()
    pools = load_pools()
    base = pools["H"] + pools["B"]
    out = []
    for bi, b in enumerate(base):
        for param, values in TARGETS.items():
            for v in values:
                m = {k: b.get(k) for k in ("ph", "temperature", "turbidity", "dissolved_oxygen",
                                           "hg_total", "cyanide", "arsenic", "altitude")}
                m[param] = v
                floor, ev = expected_floor(m, pol)
                payload = {"deviceId": f"INJ-{len(out):05d}", "latitude": 7.30, "longitude": -72.90,
                           "clusterId": "INYECCION", "timestamp": "2026-01-15T12:00:00Z",
                           **{k: x for k, x in m.items() if x is not None}}
                out.append({"sensorType": "multi_parameter_sensor", "payload": payload,
                            "meta": {"base": b["_src"], "target": param, "value": v,
                                     "oracle_floor": floor, "oracle_factors": ev}})
    OUT.write_text(json.dumps(out, ensure_ascii=False, indent=1), encoding="utf-8")
    from collections import Counter
    print("variantes:", len(out), "base:", len(base))
    print("piso esperado por el oráculo:", Counter(r["meta"]["oracle_floor"] for r in out))


if __name__ == "__main__":
    main()
