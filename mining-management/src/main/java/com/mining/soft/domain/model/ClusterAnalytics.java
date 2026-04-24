package com.mining.soft.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClusterAnalytics {
    private String clusterId;
    private long totalMeasurements;
    private long criticalAlertCount;

    public double getSai() {
        return totalMeasurements == 0 ? 0.0 : (double) criticalAlertCount / totalMeasurements;
    }
}