package com.mining.soft.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Medición ambiental (sensor in situ, laboratorio o ambos).
 *
 * FILOSOFÍA: solo se almacena lo medido; nada se imputa.
 *  - Un valor ausente queda en null.
 *  - Un valor bajo el límite de detección ("&lt; LD") también queda en null, pero
 *    se registra en {@code censoredFields} con su LD: es una medición, no un dato faltante.
 *  - Un valor físicamente implausible no se usa ni se guarda en su columna; se
 *    registra en {@code qualityFlags} con el valor original.
 *
 * OBLIGATORIOS: deviceId, latitude, longitude y al menos un parámetro medido.
 *
 * Unidades canónicas:
 *  - hgDissolved, hgTotal, methylmercury : µg/L
 *  - cyanide, arsenic                    : mg/L
 *  - hgSediment                          : mg/kg peso seco
 *  - hgFish                              : mg/kg peso húmedo
 *  - redox                               : mV (Eh vs. SHE)
 *  - organicCarbon                       : % (registrado, no puntuado)
 *  - dissolvedOxygen                     : mg/L
 *  - turbidity                           : NTU
 *  - temperature                         : °C
 *  - altitude                            : m s. n. m.
 */
@Getter @Setter
public class EnvironmentalData {
    private Long id;
    private String deviceId;
    private Double latitude;
    private Double longitude;
    private String clusterId;
    private Double altitude;
    private String thermalProfile;      // FRIA | CALIDA
    private Boolean fishPredatory;      // especie predadora (Res. 122/2012, tabla 3, núm. 13)

    // ── Fisicoquímicas in situ ─────────────────────────────────────────────
    private Double ph;
    private Double turbidity;
    private Double temperature;
    private Double redox;
    private Double dissolvedOxygen;
    private Double organicCarbon;

    // ── Contaminantes en agua ──────────────────────────────────────────────
    private Double hgDissolved;
    private Double hgTotal;
    private Double methylmercury;
    private Double cyanide;
    private Double arsenic;

    // ── Otros compartimentos (laboratorio) ─────────────────────────────────
    private Double hgSediment;
    private Double hgFish;

    // ── Resultado del modelo de alerta ─────────────────────────────────────
    private Double humanRiskScore;      // null si la dimensión no tuvo factores concluyentes
    private Double ecologicalRiskScore;
    private Double geochemicalRiskScore;
    private Double totalRiskScore;

    private String alertLevel;          // INDETERMINADO | NORMAL | PRECAUCION | ALERTA | CRITICO
    private String alertType;           // IMPACTO | RIESGO | MIXTO | NINGUNO | DESCONOCIDO
    private String levelBasis;          // PUNTAJE | NORMATIVO (el piso normativo elevó el nivel)
    private String exceedances;         // CSV "factor:NIVEL" (p. ej. "cn-agua-humano:CRITICO")

    // ── Metadatos de calidad ───────────────────────────────────────────────
    private Double dataCompleteness;    // factores concluyentes / factores del modelo
    private String missingFields;       // parámetros del modelo no reportados (CSV)
    private String censoredFields;      // CSV "parametro<LD" en unidad canónica
    private String inconclusiveFields;  // factores no concluyentes (CSV), p. ej. LD > umbral
    private String qualityFlags;        // CSV, p. ej. "IMPLAUSIBLE:dissolved_oxygen=419"
    private String dataSource;          // SENSOR | LABORATORIO | MIXTO

    private LocalDateTime timestamp;    // momento de la medición (fuente)
    private LocalDateTime ingestedAt;   // momento de ingesta en el servidor (UTC)

    // ── Estado transitorio de la ingesta (no persistido) ───────────────────
    @JsonIgnore
    private transient Map<String, Double> censoredLimits = new LinkedHashMap<>();
    @JsonIgnore
    private transient Set<String> implausibleParameters = new LinkedHashSet<>();
    @JsonIgnore
    private transient long ingestedAtEpochMs;

    /** Valor canónico de un parámetro por nombre (snake_case), o null. */
    public Double valueOf(String parameter) {
        if (parameter == null) return null;
        return switch (parameter) {
            case "ph" -> ph;
            case "turbidity" -> turbidity;
            case "temperature" -> temperature;
            case "redox" -> redox;
            case "dissolved_oxygen" -> dissolvedOxygen;
            case "organic_carbon" -> organicCarbon;
            case "hg_dissolved" -> hgDissolved;
            case "hg_total" -> hgTotal;
            case "methylmercury" -> methylmercury;
            case "cyanide" -> cyanide;
            case "arsenic" -> arsenic;
            case "hg_sediment" -> hgSediment;
            case "hg_fish" -> hgFish;
            default -> null;
        };
    }

    public void setValue(String parameter, Double value) {
        switch (parameter) {
            case "ph" -> ph = value;
            case "turbidity" -> turbidity = value;
            case "temperature" -> temperature = value;
            case "redox" -> redox = value;
            case "dissolved_oxygen" -> dissolvedOxygen = value;
            case "organic_carbon" -> organicCarbon = value;
            case "hg_dissolved" -> hgDissolved = value;
            case "hg_total" -> hgTotal = value;
            case "methylmercury" -> methylmercury = value;
            case "cyanide" -> cyanide = value;
            case "arsenic" -> arsenic = value;
            case "hg_sediment" -> hgSediment = value;
            case "hg_fish" -> hgFish = value;
            default -> throw new IllegalArgumentException("Parámetro desconocido: " + parameter);
        }
    }

    /** Parámetros de medición reconocidos por el modelo, en snake_case. */
    public static final java.util.List<String> PARAMETERS = java.util.List.of(
            "ph", "turbidity", "temperature", "redox", "dissolved_oxygen", "organic_carbon",
            "hg_dissolved", "hg_total", "methylmercury", "cyanide", "arsenic",
            "hg_sediment", "hg_fish");

    public boolean isCensored(String parameter) {
        return censoredLimits != null && censoredLimits.containsKey(parameter);
    }

    public boolean isImplausible(String parameter) {
        return implausibleParameters != null && implausibleParameters.contains(parameter);
    }
}
