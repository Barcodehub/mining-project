package com.mining.soft.domain.model;

import java.util.List;

/**
 * Evento de alerta ambiental publicado en el bus (tópico environmental-alerts).
 * {@code ingestedAtEpochMs} marca la entrada de la medición al servidor y permite
 * medir la latencia de punta a punta (ingesta → notificación persistida).
 */
public record EnvironmentalAlertEvent(
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

    public static final String TYPE = "ENVIRONMENTAL_ALERT";

    public static EnvironmentalAlertEvent from(EnvironmentalData d, long publishedAtEpochMs) {
        return new EnvironmentalAlertEvent(
                TYPE, d.getId(), d.getClusterId(), d.getDeviceId(), d.getLatitude(), d.getLongitude(),
                d.getAlertLevel(), d.getAlertType(), d.getLevelBasis(), d.getTotalRiskScore(),
                d.getHumanRiskScore(), d.getEcologicalRiskScore(), d.getGeochemicalRiskScore(),
                d.getExceedances() == null ? List.of() : List.of(d.getExceedances().split(",")),
                d.getTimestamp() == null ? null : d.getTimestamp().toString(),
                d.getIngestedAtEpochMs(), publishedAtEpochMs);
    }
}
