package com.mining.soft.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Alerta ambiental recibida del tópico environmental-alerts (publicada por mining-management). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EnvironmentalAlert(
        String eventType,
        Long measurementId,
        String clusterId,
        String deviceId,
        Double latitude,
        Double longitude,
        String alertLevel,
        String alertType,
        String levelBasis,
        Double totalRiskScore,
        Double humanRiskScore,
        Double ecologicalRiskScore,
        Double geochemicalRiskScore,
        List<String> exceedances,
        String measurementTimestamp,
        long ingestedAtEpochMs,
        long publishedAtEpochMs) {
}
