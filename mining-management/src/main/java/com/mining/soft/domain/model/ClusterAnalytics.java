package com.mining.soft.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Índice de alerta por sensores (SAI) de un clúster:
 * SAI = mediciones en ALERTA o CRITICO / mediciones clasificadas (se excluye INDETERMINADO).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClusterAnalytics {
    private String clusterId;
    private long classifiedMeasurements;
    private long alertCount;      // ALERTA + CRITICO
    private long criticalCount;   // solo CRITICO

    public double getSai() {
        return classifiedMeasurements == 0 ? 0.0 : (double) alertCount / classifiedMeasurements;
    }
}
