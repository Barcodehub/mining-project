package com.mining.soft.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class EnvironmentalDataFilter {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Double minLat;
    private Double maxLat;
    private Double minLng;
    private Double maxLng;

    // Fisicoquímicas
    private Double phMin;
    private Double phMax;
    private Double turbidityMin;
    private Double turbidityMax;
    private Double temperatureMin;
    private Double temperatureMax;
    private Double redoxMin;
    private Double redoxMax;
    private Double dissolvedOxygenMin;
    private Double organicCarbonMin;

    // Mercurio fraccionado
    private Double hgDisssolvedMin;
    private Double hgTotalMin;
    private Double hgSedimentMin;
    private Double hgFishMin;
    private Double methylmercuryMin;

    // Nivel y tipo de alerta
    private String alertLevel;
    private String alertType;
}
