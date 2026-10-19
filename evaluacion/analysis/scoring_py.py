"""
Réplica en Python del AlertScoringService v2 (Java), parametrizable por política.

Uso en el análisis:
  1. Verificación cruzada: debe reproducir exactamente nivel, tipo y puntajes que devolvió la API
     (validate_against_api en make_all.py).
  2. Sensibilidad: se re-clasifican las mismas mediciones con políticas alternativas (pesos, cortes,
     perfil térmico, regla del piso normativo) sin reiniciar el sistema.

Entrada: medición en unidades canónicas con valores numéricos, '<LD' o None; 'implausible' = set.
"""
import copy

LEVELS = ["INDETERMINADO", "NORMAL", "PRECAUCION", "ALERTA", "CRITICO"]
PARAMS = ["ph", "turbidity", "temperature", "redox", "dissolved_oxygen", "organic_carbon", "hg_dissolved",
          "hg_total", "methylmercury", "cyanide", "arsenic", "hg_sediment", "hg_fish"]
IN_SITU_DEFAULT = ["ph", "temperature", "turbidity", "dissolved_oxygen", "redox"]


def _mx(a, b):
    return a if LEVELS.index(a) >= LEVELS.index(b) else b


def parse(v):
    """→ (valor, LD) en unidades canónicas."""
    if v is None:
        return None, None
    if isinstance(v, (int, float)):
        return float(v), None
    s = str(v).strip()
    if s.startswith("<"):
        return None, float(s[1:])
    if s.startswith(">"):
        return float(s[1:]), None
    try:
        return float(s), None
    except ValueError:
        return None, None


def classify(m, pol, floor_rule="humano"):
    """
    m: dict canónico (+ 'altitude', 'thermal_profile', 'fish_predatory', '_implausible' set)
    floor_rule: 'humano' (v2: CRITICO humano → ≥ALERTA) | 'normativo_co' (cualquier CRITICO de un factor
                con fuente colombiana → ≥ALERTA) | 'ninguno'
    """
    implaus = m.get("_implausible", set())
    plaus = {k.strip("[]"): v for k, v in pol.get("plausibilidad", {}).items()}
    vals, lods = {}, {}
    for p in PARAMS:
        v, lod = parse(m.get(p))
        if v is not None and p in plaus and not (plaus[p][0] <= v <= plaus[p][1]):
            implaus = set(implaus) | {p}
            v = None
        if v is not None:
            vals[p] = v
        if lod is not None:
            lods[p] = lod

    prof = m.get("thermal_profile")
    if prof not in ("FRIA", "CALIDA"):
        alt = m.get("altitude")
        alt = parse(alt)[0] if alt is not None else None
        if alt is not None:
            prof = "FRIA" if alt >= pol["perfil-termico"]["altitud-fria-min"] else "CALIDA"
        else:
            prof = pol["perfil-termico"]["por-defecto"]
    pred = bool(m.get("fish_predatory"))
    sa, sc = pol.get("puntaje-alerta", 0.5), pol.get("puntaje-critico", 1.0)

    dim, exceed, inconc, missing = {}, [], [], []
    conclusive = 0
    cont = cond = human_crit = co_crit = False
    for f in pol["factores"]:
        p, higher = f["parametro"], f["direccion"] == "MAYOR"
        alert = f.get("alerta-fria" if prof == "FRIA" else "alerta-calida") or f.get("alerta")
        if alert is None:
            alert = f.get("alerta-calida") or f.get("alerta-fria")
        crit = f["critico-predador"] if (pred and f.get("critico-predador") is not None) else f["critico"]

        def score(v):
            if (v > crit) if higher else (v < crit):
                return sc
            if (v > alert) if higher else (v < alert):
                return sa
            return 0.0

        status, s = None, 0.0
        if p in implaus:
            status = "NC"
        elif p in vals:
            status, s = "C", score(vals[p])
        elif p in lods:
            lod = lods[p]
            if higher:
                status = "C" if lod <= alert else "NC"
            else:
                status, s = ("C", sc) if lod <= crit else (("C", sa) if lod <= alert else ("NC", 0.0))
        elif f.get("proxy") and f["proxy"] not in implaus:
            q = f["proxy"]
            if q in vals:
                s = score(vals[q])
                status = "C" if s > 0 else "NC"
            elif q in lods:
                status = "NC"
        if status is None:
            missing.append(p)
            continue
        if status == "NC":
            inconc.append(f["id"])
            continue
        conclusive += 1
        dim[f["dimension"]] = max(dim.get(f["dimension"], 0.0), s)
        if s > 0:
            lvl = "CRITICO" if s >= sc else "ALERTA"
            exceed.append(f"{f['id']}:{lvl}")
            if f["tipo"] == "CONTAMINANTE":
                cont = True
            else:
                cond = True
            if lvl == "CRITICO":
                if f["dimension"] == "HUMANA":
                    human_crit = True
                if f.get("_fuente_colombiana"):
                    co_crit = True

    res = {"humanRiskScore": dim.get("HUMANA"), "ecologicalRiskScore": dim.get("ECOLOGICA"),
           "geochemicalRiskScore": dim.get("GEOQUIMICA"), "exceedances": exceed, "thermalProfile": prof}
    if conclusive == 0:
        res.update(totalRiskScore=None, alertLevel="INDETERMINADO", alertType="DESCONOCIDO", levelBasis="PUNTAJE")
        return res
    num = sum(pol["pesos"][d] * v for d, v in dim.items())
    den = sum(pol["pesos"][d] for d in dim)
    total = num / den if den else 0.0
    n = pol["niveles"]
    by = "CRITICO" if total >= n["critico"] else "ALERTA" if total >= n["alerta"] else \
        "PRECAUCION" if total >= n["precaucion"] else "NORMAL"
    floor = "NORMAL"
    if floor_rule == "humano" and human_crit:
        floor = _mx(floor, pol["escalamiento"]["humano-critico"])
    if floor_rule == "normativo_co" and (human_crit or co_crit):
        floor = _mx(floor, "ALERTA")
    if floor_rule == "cualquier_critico" and any(e.endswith(":CRITICO") for e in exceed):
        floor = _mx(floor, "ALERTA")
    if floor_rule != "ninguno" and exceed:
        floor = _mx(floor, pol["escalamiento"]["cualquier-alerta"])
    lvl = _mx(by, floor)
    res.update(totalRiskScore=round(total, 3), alertLevel=lvl, levelBasis="PUNTAJE" if lvl == by else "NORMATIVO",
               alertType="MIXTO" if cont and cond else "IMPACTO" if cont else "RIESGO" if cond else "NINGUNO")
    return res


# Factores cuyo umbral CRÍTICO proviene de una norma colombiana (para la regla 'normativo_co').
COLOMBIAN_CRITICAL = {"hg-agua-humano", "cn-agua-humano", "as-agua-humano", "hg-pescado", "ph-bajo"}


def variant(pol, **changes):
    """Copia de la política con cambios: pesos=(h,e,g), cortes=(p,a,c), altitud_fria=…"""
    q = copy.deepcopy(pol)
    for f in q["factores"]:
        f["_fuente_colombiana"] = f["id"] in COLOMBIAN_CRITICAL
    if "pesos" in changes:
        h, e, g = changes["pesos"]
        q["pesos"] = {"HUMANA": h, "ECOLOGICA": e, "GEOQUIMICA": g}
    if "cortes" in changes:
        p, a, c = changes["cortes"]
        q["niveles"] = {"precaucion": p, "alerta": a, "critico": c}
    if "altitud_fria" in changes:
        q["perfil-termico"]["altitud-fria-min"] = changes["altitud_fria"]
    return q
