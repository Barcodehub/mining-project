package com.mining.soft.infrastructure.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "adapters", indexes = {
        @Index(name = "idx_adapter_sensor_type", columnList = "sensorType")
})
@Getter @Setter
public class AdapterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sensorType;

    @Column(columnDefinition = "TEXT")
    private String mappingJson; // guardas el JSON tal cual

    private LocalDateTime createdAt;
}