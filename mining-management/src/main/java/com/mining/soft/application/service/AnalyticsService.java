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
        List<Double> sai = fusions.stream().map(FusionAnalytics::getSai).collect(Collectors.toList());
        List<Double> cri = fusions.stream().map(FusionAnalytics::getCri).collect(Collectors.toList());
        SpearmanCorrelation.Result r = SpearmanCorrelation.compute(sai, cri);
        return new CorrelationAnalytics(r.rho(), r.pValue(), r.n(), r.exact(), r.permutations());
    }
}
