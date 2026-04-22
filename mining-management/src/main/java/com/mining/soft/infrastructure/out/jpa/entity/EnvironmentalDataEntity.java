package com.mining.soft.infrastructure.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "environmental_data", indexes = {
    @Index(name = "idx_env_data_timestamp", columnList = "timestamp"),
    @Index(name = "idx_env_data_lat_lng", columnList = "latitude, longitude"),
    @Index(name = "idx_env_data_mercury", columnList = "mercury"),
    @Index(name = "idx_env_data_ph", columnList = "ph"),
    @Index(name = "idx_env_data_turbidity", columnList = "turbidity"),
    @Index(name = "idx_env_data_temperature", columnList = "temperature")
})
@Getter @Setter
public class EnvironmentalDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceId;

    private Double latitude;
    private Double longitude;

    private Double ph;
    private Double turbidity;
    private Double temperature;
    private Double mercury;

    private LocalDateTime timestamp;
}