package com.mining.soft.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mining.soft.application.api.IEnvironmentalDataServicePort;
import com.mining.soft.application.service.AlertScoringService;
import com.mining.soft.application.service.MeasurementParser;
import com.mining.soft.application.service.MeasurementParser.Parsed;
import com.mining.soft.application.spi.IAlertEventPort;
import com.mining.soft.application.spi.IEnvironmentalDataPersistencePort;
import com.mining.soft.domain.model.Adapter;
import com.mining.soft.domain.model.EnvironmentalAggregate;
import com.mining.soft.domain.model.EnvironmentalAlertEvent;
import com.mining.soft.domain.model.EnvironmentalData;
import com.mining.soft.domain.model.EnvironmentalDataFilter;
import com.mining.soft.infrastructure.configuration.AmbientalConfig;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnvironmentalDataUseCase implements IEnvironmentalDataServicePort {

    private final IEnvironmentalDataPersistencePort persistencePort;
    private final AlertScoringService alertScoringService;
    private final IAlertEventPort alertEventPort;
    private final AmbientalConfig cfg;
    private final ObjectMapper objectMapper;

    public EnvironmentalDataUseCase(IEnvironmentalDataPersistencePort persistencePort,
                                    AlertScoringService alertScoringService,
                                    IAlertEventPort alertEventPort,
                                    AmbientalConfig cfg) {
        this.persistencePort = persistencePort;
        this.alertScoringService = alertScoringService;
        this.alertEventPort = alertEventPort;
        this.cfg = cfg;
        this.objectMapper = new ObjectMapper();
    }

    /** Mapeo de un campo canónico a la ruta en el payload, con factor de conversión de unidad. */
    record FieldMapping(String path, double factor) { }

    @Override
    public EnvironmentalData processSensorData(String sensorType, Map<String, Object> payload) {
        long ingestedAtMs = System.currentTimeMillis();
        Map<String, FieldMapping> mapping = resolveMapping(sensorType);
        List<Double> sentinels = cfg.getValoresAusentes();
        List<String> flags = new ArrayList<>();

        EnvironmentalData data = new EnvironmentalData();
        data.setIngestedAtEpochMs(ingestedAtMs);
        data.setIngestedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(ingestedAtMs), ZoneOffset.UTC));

        // ── Metadatos ──────────────────────────────────────────────────────
        data.setDeviceId(asString(extract(payload, mapping.get("device_id"))));
        data.setClusterId(asString(extract(payload, mapping.get("cluster_id"))));
        data.setLatitude(numeric(payload, mapping.get("lat"), "latitude", sentinels, flags));
        data.setLongitude(numeric(payload, mapping.get("lng"), "longitude", sentinels, flags));
        data.setAltitude(numeric(payload, mapping.get("altitude"), "altitude", sentinels, flags));
        data.setThermalProfile(asString(extract(payload, mapping.get("thermal_profile"))));
        Object predator = extract(payload, mapping.get("fish_predatory"));
        if (predator != null) data.setFishPredatory(Boolean.parseBoolean(predator.toString()));

        // ── Parámetros medidos ─────────────────────────────────────────────
        int measured = 0;
        for (String p : EnvironmentalData.PARAMETERS) {
            FieldMapping fm = mapping.get(p);
            Parsed parsed = MeasurementParser.parse(extract(payload, fm), fm == null ? 1.0 : fm.factor(), sentinels);
            switch (parsed.kind()) {
                case ABSENT -> { }
                case UNPARSEABLE -> flags.add("UNPARSEABLE:" + p + "=" + parsed.raw());
                case CENSORED -> {
                    data.getCensoredLimits().put(p, parsed.value());
                    measured++;
                }
                case VALUE -> {
                    if (isPlausible(p, parsed.value())) {
                        data.setValue(p, parsed.value());
                        if (parsed.rightCensored()) flags.add("ABOVE_RANGE:" + p);
                    } else {
                        data.getImplausibleParameters().add(p);
                        flags.add("IMPLAUSIBLE:" + p + "=" + parsed.raw());
                    }
                    measured++;
                }
            }
        }

        // ── Timestamp de la medición ───────────────────────────────────────
        data.setTimestamp(parseTimestamp(extract(payload, mapping.get("timestamp")), flags));

        // ── Validación mínima ──────────────────────────────────────────────
        if (data.getDeviceId() == null || data.getLatitude() == null || data.getLongitude() == null) {
            throw new IllegalArgumentException(
                    "Campos obligatorios faltantes o implausibles: deviceId, lat, lng");
        }
        if (measured == 0) {
            throw new IllegalArgumentException("La medición no contiene ningún parámetro reconocido");
        }

        data.setCensoredFields(data.getCensoredLimits().isEmpty() ? null
                : String.join(",", data.getCensoredLimits().entrySet().stream()
                        .map(e -> e.getKey() + "<" + e.getValue()).toList()));
        data.setQualityFlags(flags.isEmpty() ? null : String.join(",", flags));

        // ── Clasificación al momento de la ingesta ─────────────────────────
        alertScoringService.calculateAndAssign(data);

        EnvironmentalData saved = persistencePort.saveEnvironmentalData(data);
        saved.setIngestedAtEpochMs(ingestedAtMs);

        if (cfg.getAlertas().getNivelesPublicados().contains(saved.getAlertLevel())) {
            alertEventPort.publish(EnvironmentalAlertEvent.from(saved, System.currentTimeMillis()));
        }
        return saved;
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
        // Valida el mapeo antes de guardarlo.
        parseMapping(adapter.getMappingJson());

        persistencePort.findAdapterBySensorType(adapter.getSensorType()).ifPresent(existing -> {
            adapter.setId(existing.getId());
            adapter.setCreatedAt(existing.getCreatedAt());
        });

        if (adapter.getCreatedAt() == null) adapter.setCreatedAt(LocalDateTime.now());
        return persistencePort.saveAdapter(adapter);
    }

    // ── Auxiliares ─────────────────────────────────────────────────────────
    private boolean isPlausible(String parameter, double v) {
        List<Double> range = cfg.getPlausibilidad().get(parameter);
        if (range == null || range.size() != 2) return true;
        return v >= range.get(0) && v <= range.get(1);
    }

    private Double numeric(Map<String, Object> payload, FieldMapping fm, String name,
                           List<Double> sentinels, List<String> flags) {
        Parsed p = MeasurementParser.parse(extract(payload, fm), fm == null ? 1.0 : fm.factor(), sentinels);
        if (p.kind() != MeasurementParser.Kind.VALUE) {
            if (p.kind() != MeasurementParser.Kind.ABSENT) flags.add("UNPARSEABLE:" + name + "=" + p.raw());
            return null;
        }
        if (!isPlausible(name, p.value())) {
            flags.add("IMPLAUSIBLE:" + name + "=" + p.raw());
            return null;
        }
        return p.value();
    }

    private LocalDateTime parseTimestamp(Object raw, List<String> flags) {
        if (raw != null) {
            String s = raw.toString().trim();
            try {
                return OffsetDateTime.parse(s).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
            } catch (Exception ignored) { /* puede venir sin zona */ }
            try {
                return LocalDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception ignored) {
                flags.add("UNPARSEABLE:timestamp=" + s);
            }
        }
        flags.add("TIMESTAMP_DEFAULTED");
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private Object extract(Map<String, Object> payload, FieldMapping fm) {
        if (fm == null || fm.path() == null) return null;
        Object current = payload;
        for (String key : fm.path().split("\\.")) {
            if (current instanceof Map<?, ?> m) current = m.get(key);
            else return null;
        }
        return current;
    }

    private String asString(Object obj) {
        return obj == null ? null : obj.toString();
    }

    private Map<String, FieldMapping> resolveMapping(String sensorType) {
        Map<String, FieldMapping> mapping = new HashMap<>(defaultMapping());
        persistencePort.findAdapterBySensorType(sensorType)
                .ifPresent(adapter -> mapping.putAll(parseMapping(adapter.getMappingJson())));
        return mapping;
    }

    /**
     * El mapeo admite dos formas por campo:
     *  "hg_total": "mercurio_mg_l"                          (ruta, factor 1)
     *  "hg_total": {"path": "mercurio_mg_l", "factor": 1000} (ruta + conversión de unidad)
     */
    Map<String, FieldMapping> parseMapping(String mappingJson) {
        Map<String, FieldMapping> out = new HashMap<>();
        if (mappingJson == null || mappingJson.isBlank()) return out;
        try {
            Map<String, Object> raw = objectMapper.readValue(mappingJson, new TypeReference<>() { });
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                Object v = e.getValue();
                if (v instanceof String path) {
                    out.put(e.getKey(), new FieldMapping(path, 1.0));
                } else if (v instanceof Map<?, ?> m && m.get("path") != null) {
                    Object f = m.get("factor");
                    double factor = f == null ? 1.0 : Double.parseDouble(f.toString());
                    out.put(e.getKey(), new FieldMapping(m.get("path").toString(), factor));
                } else {
                    throw new IllegalArgumentException("Mapeo inválido para '" + e.getKey() + "'");
                }
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("mapping.json del adapter no es JSON válido", ex);
        }
        return out;
    }

    private Map<String, FieldMapping> defaultMapping() {
        Map<String, FieldMapping> m = new HashMap<>();
        m.put("device_id", new FieldMapping("deviceId", 1.0));
        m.put("lat", new FieldMapping("latitude", 1.0));
        m.put("lng", new FieldMapping("longitude", 1.0));
        m.put("cluster_id", new FieldMapping("clusterId", 1.0));
        m.put("altitude", new FieldMapping("altitude", 1.0));
        m.put("thermal_profile", new FieldMapping("thermal_profile", 1.0));
        m.put("fish_predatory", new FieldMapping("fish_predatory", 1.0));
        m.put("timestamp", new FieldMapping("timestamp", 1.0));
        for (String p : EnvironmentalData.PARAMETERS) m.put(p, new FieldMapping(p, 1.0));
        return m;
    }
}
