package com.mining.soft.infrastructure.input.dto;
import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter @Setter
public class SensorDataRequestDto {
    private String sensorType;
    private Map<String, Object> payload;
}
