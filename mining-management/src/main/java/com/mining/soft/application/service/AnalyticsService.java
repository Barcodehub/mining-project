package com.mining.soft.application.service;

import com.mining.soft.domain.model.*;
import com.mining.soft.infrastructure.out.jpa.repository.IEnvironmentalDataRepository;
import com.mining.soft.infrastructure.out.jpa.repository.IPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final IEnvironmentalDataRepository environmentalDataRepository;
    private final IPostRepository postRepository;

    public List<ClusterAnalytics> getClusterAnalytics() {
        return environmentalDataRepository.getClusterAnalytics();
    }

    public List<ReportAnalytics> getReportAnalytics() {
        return postRepository.getReportAnalytics();
    }

    public List<FusionAnalytics> getFusionAnalytics() {
        Map<String, Double> saiMap = getClusterAnalytics().stream()
                .collect(Collectors.toMap(ClusterAnalytics::getClusterId, ClusterAnalytics::getSai));

        return getReportAnalytics().stream()
                .filter(r -> saiMap.containsKey(r.getClusterId()))
                .map(r -> new FusionAnalytics(r.getClusterId(), saiMap.get(r.getClusterId()), r.getCri()))
                .collect(Collectors.toList());
    }

    public CorrelationAnalytics getCorrelationAnalytics() {
        List<FusionAnalytics> fusions = getFusionAnalytics();
        if (fusions.isEmpty()) return new CorrelationAnalytics(0.0, null);

        List<Double> sai = fusions.stream().map(FusionAnalytics::getSai).collect(Collectors.toList());
        List<Double> cri = fusions.stream().map(FusionAnalytics::getCri).collect(Collectors.toList());
        return new CorrelationAnalytics(calculateSpearman(sai, cri), null);
    }

    private double calculateSpearman(List<Double> x, List<Double> y) {
        int n = x.size();
        if (n <= 1) return 0.0;
        List<Double> rx = rank(x), ry = rank(y);
        double sumD2 = 0.0;
        for (int i = 0; i < n; i++) { double d = rx.get(i) - ry.get(i); sumD2 += d * d; }
        return 1.0 - (6.0 * sumD2) / ((double) n * (n * n - 1));
    }

    private List<Double> rank(List<Double> values) {
        int n = values.size();
        // Build array of (value, originalIndex)
        java.util.List<java.util.Map.Entry<Double, Integer>> entries = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) entries.add(new java.util.AbstractMap.SimpleEntry<>(values.get(i), i));
        // Sort by value
        entries.sort(java.util.Comparator.comparingDouble(java.util.Map.Entry::getKey));

        double[] ranks = new double[n];
        int i = 0;
        while (i < n) {
            int j = i;
            double v = entries.get(i).getKey();
            while (j + 1 < n && Double.compare(entries.get(j + 1).getKey(), v) == 0) j++;
            // average rank for ties: (i+1 .. j+1)
            double avgRank = ( (double)(i + 1) + (double)(j + 1) ) / 2.0;
            for (int k = i; k <= j; k++) {
                ranks[entries.get(k).getValue()] = avgRank;
            }
            i = j + 1;
        }

        java.util.List<Double> result = new java.util.ArrayList<>(n);
        for (int idx = 0; idx < n; idx++) result.add(ranks[idx]);
        return result;
    }
}
