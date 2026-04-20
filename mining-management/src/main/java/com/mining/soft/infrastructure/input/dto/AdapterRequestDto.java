package com.mining.soft.infrastructure.input.dto;
import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter @Setter
public class AdapterRequestDto {
    private String sensorType;
    private Map<String, String> mapping;
}
