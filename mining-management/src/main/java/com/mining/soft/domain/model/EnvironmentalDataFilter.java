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
    private Double phMin;
    private Double phMax;
    private Double turbidityMin;
    private Double turbidityMax;
    private Double temperatureMin;
    private Double temperatureMax;
    private Double mercuryMin;
    private Double mercuryMax;
}
