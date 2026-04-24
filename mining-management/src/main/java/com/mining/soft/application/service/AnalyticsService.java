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
        List<ClusterAnalytics> clusters = getClusterAnalytics();
        List<ReportAnalytics> reports = getReportAnalytics();

        Map<String, Double> saiMap = clusters.stream()
                .collect(Collectors.toMap(ClusterAnalytics::getClusterId, ClusterAnalytics::getSai));

        return reports.stream()
                .filter(r -> saiMap.containsKey(r.getClusterId()))
                .map(r -> new FusionAnalytics(r.getClusterId(), saiMap.get(r.getClusterId()), r.getCri()))
                .collect(Collectors.toList());
    }

    public CorrelationAnalytics getCorrelationAnalytics() {
        List<FusionAnalytics> fusions = getFusionAnalytics();

        if (fusions.isEmpty()) {
            return new CorrelationAnalytics(0.0, null);
        }

        // Implementar correlación Spearman manualmente
        List<Double> saiList = fusions.stream().map(FusionAnalytics::getSai).collect(Collectors.toList());
        List<Double> criList = fusions.stream().map(FusionAnalytics::getCri).collect(Collectors.toList());

        double spearman = calculateSpearman(saiList, criList);
        return new CorrelationAnalytics(spearman, null); // p-value opcional, no implementado
    }

    private double calculateSpearman(List<Double> x, List<Double> y) {
        int n = x.size();
        if (n <= 1) return 0.0;

        // Rank x
        List<Double> rankX = rank(x);
        List<Double> rankY = rank(y);

        // Calculate d^2
        double sumD2 = 0.0;
        for (int i = 0; i < n; i++) {
            double d = rankX.get(i) - rankY.get(i);
            sumD2 += d * d;
        }

        // Spearman = 1 - (6 * sumD2) / (n * (n^2 - 1))
        return 1.0 - (6.0 * sumD2) / (n * (n * n - 1));
    }

    private List<Double> rank(List<Double> values) {
        List<Double> sorted = values.stream().sorted().collect(Collectors.toList());
        return values.stream()
                .map(v -> (double) (sorted.indexOf(v) + 1))
                .collect(Collectors.toList());
    }
}