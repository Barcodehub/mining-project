package com.mining.soft.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mining.soft.application.api.IEnvironmentalDataServicePort;
import com.mining.soft.application.service.AlertScoringService;
import com.mining.soft.application.spi.IEnvironmentalDataPersistencePort;
import com.mining.soft.domain.model.Adapter;
import com.mining.soft.domain.model.EnvironmentalAggregate;
import com.mining.soft.domain.model.EnvironmentalData;
import com.mining.soft.domain.model.EnvironmentalDataFilter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnvironmentalDataUseCase implements IEnvironmentalDataServicePort {

    private final IEnvironmentalDataPersistencePort persistencePort;
    private final AlertScoringService alertScoringService;
    private final ObjectMapper objectMapper;

    public EnvironmentalDataUseCase(IEnvironmentalDataPersistencePort persistencePort,
                                    AlertScoringService alertScoringService) {
        this.persistencePort = persistencePort;
        this.alertScoringService = alertScoringService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public EnvironmentalData processSensorData(String sensorType, Map<String, Object> payload) {
        Map<String, String> mapping = resolveMapping(sensorType);

        EnvironmentalData data = new EnvironmentalData();
        
        // ── Metadatos obligatorios ─────────────────────────────────────────
        data.setDeviceId(asString(extractValue(payload, mapping.get("device_id"))));
        data.setLatitude(asDouble(extractValue(payload, mapping.get("lat"))));
        data.setLongitude(asDouble(extractValue(payload, mapping.get("lng"))));
        data.setClusterId(asString(extractValue(payload, mapping.get("cluster_id"))));

        // ── Fisicoquímicas básicas (OBLIGATORIAS) ──────────────────────────
        data.setPh(asDouble(extractValue(payload, mapping.get("ph"))));
        data.setTurbidity(asDouble(extractValue(payload, mapping.get("turbidity"))));
        data.setTemperature(asDouble(extractValue(payload, mapping.get("temperature"))));

        // ── Geoquímicas avanzadas (OPCIONALES) ─────────────────────────────
        data.setRedox(asDouble(extractValue(payload, mapping.get("redox"))));
        data.setDissolvedOxygen(asDouble(extractValue(payload, mapping.get("dissolved_oxygen"))));
        data.setOrganicCarbon(asDouble(extractValue(payload, mapping.get("organic_carbon"))));

        // ── Mercurio fraccionado (SOLO LO QUE VIENE EN EL PAYLOAD) ─────────
        // NO calculamos nada. Si no viene, queda null.
        data.setHgDissolved(asDouble(extractValue(payload, mapping.get("hg_dissolved"))));
        data.setHgTotal(asDouble(extractValue(payload, mapping.get("hg_total"))));
        data.setHgSediment(asDouble(extractValue(payload, mapping.get("hg_sediment"))));
        data.setHgFish(asDouble(extractValue(payload, mapping.get("hg_fish"))));
        data.setMethylmercury(asDouble(extractValue(payload, mapping.get("methylmercury"))));

        // ── Timestamp ───────────────────────────────────────────────────────
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

        // ── Validación: Al menos un tipo de mercurio debe estar presente ───
        if (data.getHgDissolved() == null && data.getHgTotal() == null) {
            throw new IllegalArgumentException(
                "Datos insuficientes: se requiere al menos 'hg_dissolved' o 'hg_total'"
            );
        }

        // ── Validación: Campos obligatorios ─────────────────────────────────
        if (data.getDeviceId() == null || data.getLatitude() == null || 
            data.getLongitude() == null || data.getPh() == null || 
            data.getTurbidity() == null || data.getTemperature() == null) {
            throw new IllegalArgumentException(
                "Campos obligatorios faltantes: deviceId, lat, lng, ph, turbidity, temperature"
            );
        }

        // ── Calcular scores de riesgo (SOLO con datos presentes) ───────────
        alertScoringService.calculateAndAssign(data);

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
        if (adapter == null || adapter.getSensorType() == null || adapter.getSensorType().isBlank()) {
            throw new IllegalArgumentException("sensorType es obligatorio");
        }

        persistencePort.findAdapterBySensorType(adapter.getSensorType()).ifPresent(existing -> {
            adapter.setId(existing.getId());
            adapter.setCreatedAt(existing.getCreatedAt());
        });

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
        try { return Double.parseDouble(obj.toString()); }
        catch (NumberFormatException e) { return null; }
    }

    private String asString(Object obj) {
        return obj == null ? null : obj.toString();
    }

    private Map<String, String> resolveMapping(String sensorType) {
        Map<String, String> mapping = new HashMap<>(defaultMapping());
        persistencePort.findAdapterBySensorType(sensorType).ifPresent(adapter -> {
            String mappingJson = adapter.getMappingJson();
            if (mappingJson == null || mappingJson.isBlank()) return;
            try {
                Map<String, String> adapterMapping = objectMapper.readValue(mappingJson, new TypeReference<>() {});
                if (adapterMapping != null) {
                    mapping.putAll(adapterMapping);
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error parseando mapping.json del adapter", e);
            }
        });
        return mapping;
    }

    private Map<String, String> defaultMapping() {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("device_id", "deviceId");
        mapping.put("lat", "latitude");
        mapping.put("lng", "longitude");
        mapping.put("cluster_id", "clusterId");
        mapping.put("ph", "ph");
        mapping.put("turbidity", "turbidity");
        mapping.put("temperature", "temperature");
        mapping.put("redox", "redox");
        mapping.put("dissolved_oxygen", "dissolved_oxygen");
        mapping.put("organic_carbon", "organic_carbon");
        mapping.put("hg_dissolved", "hg_dissolved");
        mapping.put("hg_total", "hg_total");
        mapping.put("hg_sediment", "hg_sediment");
        mapping.put("hg_fish", "hg_fish");
        mapping.put("methylmercury", "methylmercury");
        mapping.put("timestamp", "timestamp");
        return mapping;
    }
}
