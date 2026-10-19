package com.mining.soft.infrastructure.input.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class EnvironmentalDataResponseDto {
    private Long id;
    private String deviceId;
    private Double latitude;
    private Double longitude;
    private String clusterId;

    // Fisicoquímicas básicas
    private Double ph;
    private Double turbidity;
    private Double temperature;

    // Geoquímicas avanzadas (pueden ser null)
    private Double redox;
    private Double dissolvedOxygen;
    private Double organicCarbon;

    // Mercurio fraccionado (pueden ser null)
    private Double hgDissolved;
    private Double hgTotal;
    private Double hgSediment;
    private Double hgFish;
    private Double methylmercury;

    // Scores de riesgo
    private Double humanRiskScore;
    private Double ecologicalRiskScore;
    private Double geochemicalRiskScore;
    private Double totalRiskScore;
    
    private String alertLevel;      // NORMAL | PRECAUCION | ALERTA | CRITICO
    private String alertType;       // IMPACTO | RIESGO | MIXTO | DESCONOCIDO

    // Metadatos de calidad
    private Double dataCompleteness;
    private String missingFields;
    private String dataSource;      // SENSOR | LABORATORIO | MIXTO

    private LocalDateTime timestamp;
}
