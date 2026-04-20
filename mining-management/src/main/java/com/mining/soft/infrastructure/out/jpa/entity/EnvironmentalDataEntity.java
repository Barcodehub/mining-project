package com.mining.soft.infrastructure.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "environmental_data")
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