package com.mining.soft.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Índice de reportes ciudadanos (CRI) de un clúster: media ponderada por estado de
 * validación (VALIDADO 1.0, PENDIENTE 0.5, SIN_VALIDAR 0.25, RECHAZADO 0.0).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportAnalytics {
    private String clusterId;
    private long totalReports;
    private double weightedReports;

    public double getCri() {
        return totalReports == 0 ? 0.0 : weightedReports / totalReports;
    }
}
