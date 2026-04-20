package com.mining.soft.infrastructure.input.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mining.soft.domain.model.Adapter;
import com.mining.soft.infrastructure.input.dto.AdapterRequestDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class IEnvironmentalDtoMapper {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Adapter toAdapterModel(AdapterRequestDto dto) {
        if (dto == null) return null;
        Adapter adapter = new Adapter();
        adapter.setSensorType(dto.getSensorType());
        try {
            adapter.setMappingJson(objectMapper.writeValueAsString(dto.getMapping()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting mapping to JSON", e);
        }
        adapter.setCreatedAt(LocalDateTime.now());
        return adapter;
    }
}
