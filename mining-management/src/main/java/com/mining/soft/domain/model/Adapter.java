package com.mining.soft.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Adapter {
    private Long id;
    private String sensorType;
    private String mappingJson;
    private LocalDateTime createdAt;
}
