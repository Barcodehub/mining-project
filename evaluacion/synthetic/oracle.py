"""
Oráculo normativo independiente del código Java.

Lee los mismos umbrales de config-ambiental.yaml y decide, para una medición en unidades canónicas,
qué factores superan su límite legal o de referencia (ALERTA / CRITICO), cuáles no y cuáles no son
concluyentes (censurado con LD > umbral). No calcula el puntaje compuesto: solo la verdad normativa
contra la que se evalúa al clasificador (V2):

  - superación enmascarada : el oráculo ve ≥ 1 superación HUMANA y el sistema reporta NORMAL o INDETERMINADO
  - falsa alarma           : el oráculo no ve ninguna superación (todo concluyente) y el sistema reporta ≥ ALERTA
"""
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[2]
CFG = ROOT / "mining-management/src/main/resources/config-ambiental.yaml"


def load_policy(path=CFG):
    return yaml.safe_load(Path(path).read_text(encoding="utf-8"))["ambiental"]


def _parse(v):
    if v is None:
        return None, None
    if isinstance(v, (int, float)):
        return float(v), None
    s = str(v).strip()
    if s.startswith("<"):
        return None, float(s[1:])
    try:
        return float(s), None
    except ValueError:
        return None, None


def _profile(m, pol):
    alt = m.get("altitude")
    if alt is None:
        return pol["perfil-termico"]["por-defecto"]
    return "FRIA" if float(alt) >= pol["perfil-termico"]["altitud-fria-min"] else "CALIDA"


def evaluate(m, pol):
    """Devuelve {factor_id: 'CRITICO' | 'ALERTA' | 'NO' | 'NC'} para los factores con dato."""
    prof = _profile(m, pol)
    plaus = {k.strip("[]"): v for k, v in pol.get("plausibilidad", {}).items()}
    out = {}
    for f in pol["factores"]:
        p = f["parametro"]
        v, lod = _parse(m.get(p))
        if v is not None and p in plaus and not (plaus[p][0] <= v <= plaus[p][1]):
            out[f["id"]] = "NC"
            continue
        alert = f.get("alerta-fria" if prof == "FRIA" else "alerta-calida", f.get("alerta"))
        if alert is None:
            alert = f.get("alerta-calida") or f.get("alerta-fria")
        crit = f["critico"]
        higher = f["direccion"] == "MAYOR"
        if v is None and lod is None and f.get("proxy"):
            pv, plod = _parse(m.get(f["proxy"]))
            if pv is not None and ((pv > alert) if higher else (pv < alert)):
                v = pv
        if v is not None:
            if (v > crit) if higher else (v < crit):
                out[f["id"]] = "CRITICO"
            elif (v > alert) if higher else (v < alert):
                out[f["id"]] = "ALERTA"
            else:
                out[f["id"]] = "NO"
        elif lod is not None:
            out[f["id"]] = "NO" if (higher and lod <= alert) else ("NC" if higher else "CRITICO" if lod <= crit else "NC")
    return out


def expected_floor(m, pol):
    """Nivel mínimo que exige la norma: ALERTA si hay CRÍTICO humano; PRECAUCION si hay cualquier superación."""
    ev = evaluate(m, pol)
    dims = {f["id"]: f["dimension"] for f in pol["factores"]}
    if any(s == "CRITICO" and dims[k] == "HUMANA" for k, s in ev.items()):
        return "ALERTA", ev
    if any(s in ("ALERTA", "CRITICO") for s in ev.values()):
        return "PRECAUCION", ev
    if ev and all(s == "NO" for s in ev.values()):
        return "NINGUNA", ev
    return "NO_CONCLUYENTE", ev


LEVELS = ["INDETERMINADO", "NORMAL", "PRECAUCION", "ALERTA", "CRITICO"]


def check(system_level, m, pol):
    """Clasifica el resultado del sistema frente al oráculo: OK | MASKED | FALSE_ALARM | FLOOR_VIOLATION."""
    floor, ev = expected_floor(m, pol)
    human_exceed = any(s in ("ALERTA", "CRITICO") and f["dimension"] == "HUMANA"
                       for f in pol["factores"] for k, s in ev.items() if k == f["id"])
    if human_exceed and system_level in ("NORMAL", "INDETERMINADO"):
        return "MASKED"
    if floor == "NINGUNA" and system_level in ("ALERTA", "CRITICO"):
        return "FALSE_ALARM"
    if floor in ("ALERTA", "PRECAUCION") and LEVELS.index(system_level) < LEVELS.index(floor):
        return "FLOOR_VIOLATION"
    return "OK"
