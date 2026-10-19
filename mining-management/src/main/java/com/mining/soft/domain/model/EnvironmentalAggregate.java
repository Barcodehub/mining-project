package com.mining.soft.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class EnvironmentalAggregate {
    // pH
    private Double avgPh;
    private Double minPh;
    private Double maxPh;
    // Turbidez
    private Double avgTurbidity;
    private Double minTurbidity;
    private Double maxTurbidity;
    // Temperatura
    private Double avgTemperature;
    private Double minTemperature;
    private Double maxTemperature;
    // Redox
    private Double avgRedox;
    private Double minRedox;
    private Double maxRedox;
    // Oxígeno disuelto
    private Double avgDissolvedOxygen;
    private Double minDissolvedOxygen;
    private Double maxDissolvedOxygen;
    // Hg disuelto (µg/L)
    private Double avgHgDissolved;
    private Double minHgDissolved;
    private Double maxHgDissolved;
    // Hg total (µg/L)
    private Double avgHgTotal;
    private Double minHgTotal;
    private Double maxHgTotal;
    // Hg sedimento (mg/kg)
    private Double avgHgSediment;
    private Double minHgSediment;
    private Double maxHgSediment;
    // Hg peces (mg/kg)
    private Double avgHgFish;
    private Double minHgFish;
    private Double maxHgFish;
    // Metilmercurio (µg/L)
    private Double avgMethylmercury;
    private Double minMethylmercury;
    private Double maxMethylmercury;
    // Scores
    private Double avgTotalRiskScore;
    private Long countCritico;
    private Long countAlerta;
}
