package com.mining.soft.domain.model;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class EnvironmentalAggregate {
    private Double avgPh;
    private Double minPh;
    private Double maxPh;
    private Double avgTurbidity;
    private Double minTurbidity;
    private Double maxTurbidity;
    private Double avgTemperature;
    private Double minTemperature;
    private Double maxTemperature;
    private Double avgMercury;
    private Double minMercury;
    private Double maxMercury;
}
