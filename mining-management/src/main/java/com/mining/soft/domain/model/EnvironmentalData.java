package com.mining.soft.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class EnvironmentalData {
    private Long id;
    private String deviceId;
    private Double latitude;
    private Double longitude;
    private Double ph;
    private Double turbidity;
    private Double temperature;
    private Double mercury;
    private LocalDateTime timestamp;
    // Getters y setters
    // ...
}
