package com.mining.soft.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mining.soft.application.api.IEnvironmentalDataServicePort;
import com.mining.soft.application.spi.IEnvironmentalDataPersistencePort;
import com.mining.soft.domain.model.Adapter;
import com.mining.soft.domain.model.EnvironmentalAggregate;
import com.mining.soft.domain.model.EnvironmentalData;
import com.mining.soft.domain.model.EnvironmentalDataFilter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class EnvironmentalDataUseCase implements IEnvironmentalDataServicePort {

    private final IEnvironmentalDataPersistencePort persistencePort;
    private final ObjectMapper objectMapper;

    public EnvironmentalDataUseCase(IEnvironmentalDataPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public EnvironmentalData processSensorData(String sensorType, Map<String, Object> payload) {
        Adapter adapter = persistencePort.findAdapterBySensorType(sensorType)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró adaptador para el sensor: " + sensorType));

        Map<String, String> mapping;
        try {
            mapping = objectMapper.readValue(adapter.getMappingJson(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parseando mapping.json del adapter", e);
        }

        EnvironmentalData data = new EnvironmentalData();
        data.setDeviceId(asString(extractValue(payload, mapping.get("device_id"))));
        data.setLatitude(asDouble(extractValue(payload, mapping.get("lat"))));
        data.setLongitude(asDouble(extractValue(payload, mapping.get("lng"))));
        data.setPh(asDouble(extractValue(payload, mapping.get("ph"))));
        data.setTurbidity(asDouble(extractValue(payload, mapping.get("turbidity"))));
        data.setTemperature(asDouble(extractValue(payload, mapping.get("temperature"))));
        data.setMercury(asDouble(extractValue(payload, mapping.get("mercury"))));
        data.setClusterId(asString(extractValue(payload, mapping.get("cluster_id"))));

        Object timeObj = extractValue(payload, mapping.get("timestamp"));
        if (timeObj != null) {
            try {
                data.setTimestamp(LocalDateTime.parse(timeObj.toString(), DateTimeFormatter.ISO_DATE_TIME));
            } catch (Exception e) {
                data.setTimestamp(LocalDateTime.now());
            }
        } else {
            data.setTimestamp(LocalDateTime.now());
        }

        return persistencePort.saveEnvironmentalData(data);
    }

    @Override
    public List<EnvironmentalData> getFilteredData(EnvironmentalDataFilter filter) {
        return persistencePort.findFiltered(filter);
    }

    @Override
    public EnvironmentalAggregate getAggregatedData(EnvironmentalDataFilter filter) {
        return persistencePort.getAggregated(filter);
    }

    @Override
    public Adapter createAdapter(Adapter adapter) {
        if (adapter.getCreatedAt() == null) adapter.setCreatedAt(LocalDateTime.now());
        return persistencePort.saveAdapter(adapter);
    }

    private Object extractValue(Map<String, Object> payload, String path) {
        if (path == null) return null;
        String[] keys = path.split("\\.");
        Object current = payload;
        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(key);
            } else {
                return null;
            }
        }
        return current;
    }

    private Double asDouble(Object obj) {
        if (obj == null) return null;
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String asString(Object obj) {
        return obj == null ? null : obj.toString();
    }
}
