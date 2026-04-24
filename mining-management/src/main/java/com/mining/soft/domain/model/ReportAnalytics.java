package com.mining.soft.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
