package com.mining.soft.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Modelo de medición ambiental con rigor científico.
 * 
 * FILOSOFÍA: Solo almacenamos datos MEDIDOS. No inventamos valores.
 * Si un campo no viene del sensor o laboratorio, queda en null.
 *
 * CAMPOS OBLIGATORIOS:
 *  - deviceId, latitude, longitude, timestamp
 *  - ph, temperature, turbidity
 *  - Al menos UNO de: hgTotal o hgDissolved
 *
 * CAMPOS OPCIONALES (null si no se midieron):
 *  - hgSediment, methylmercury, hgFish
 *  - redox, dissolvedOxygen, organicCarbon
 *
 * Unidades:
 *  - hgDissolved, hgTotal, methylmercury : µg/L
 *  - hgSediment, hgFish                  : mg/kg
 *  - redox                               : mV
 *  - organicCarbon                       : % peso seco
 *  - dissolvedOxygen                     : mg/L
 *  - turbidity                           : NTU
 *  - temperature                         : °C
 */
@Getter @Setter
public class EnvironmentalData {
    private Long id;
    private String deviceId;
    private Double latitude;
    private Double longitude;
    private String clusterId;

    // ── Fisicoquímicas básicas (OBLIGATORIAS) ──────────────────────────────
    private Double ph;
    private Double turbidity;
    private Double temperature;

    // ── Geoquímicas avanzadas (OPCIONALES) ─────────────────────────────────
    private Double redox;               // mV — potencial redox
    private Double dissolvedOxygen;     // mg/L
    private Double organicCarbon;       // % peso seco

    // ── Mercurio fraccionado (AL MENOS UNO OBLIGATORIO) ────────────────────
    private Double hgDissolved;         // µg/L — Hg disuelto en agua
    private Double hgTotal;             // µg/L — Hg total en agua
    
    // ── Mercurio en otros compartimentos (OPCIONALES - solo laboratorio) ───
    private Double hgSediment;          // mg/kg — Hg en sedimento
    private Double hgFish;              // mg/kg — Hg en peces
    private Double methylmercury;       // µg/L — MeHg en agua

    // ── Índices de riesgo calculados ────────────────────────────────────────
    private Double humanRiskScore;
    private Double ecologicalRiskScore;
    private Double geochemicalRiskScore;
    private Double totalRiskScore;
    
    private String alertLevel;          // NORMAL | PRECAUCION | ALERTA | CRITICO
    private String alertType;           // IMPACTO | RIESGO | MIXTO | DESCONOCIDO
    
    // ── Metadatos de calidad de datos ──────────────────────────────────────
    private Double dataCompleteness;    // % de campos opcionales presentes (0.0 - 1.0)
    private String missingFields;       // Lista de campos críticos faltantes (CSV)
    private String dataSource;          // SENSOR | LABORATORIO | MIXTO

    private LocalDateTime timestamp;
}
