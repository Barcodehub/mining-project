package com.mining.soft.infrastructure.input;

import com.mining.soft.application.service.AnalyticsService;
import com.mining.soft.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/clusters")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClusterAnalytics>> getClusterAnalytics() {
        List<ClusterAnalytics> result = analyticsService.getClusterAnalytics();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/reports")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportAnalytics>> getReportAnalytics() {
        List<ReportAnalytics> result = analyticsService.getReportAnalytics();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/fusion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FusionAnalytics>> getFusionAnalytics() {
        List<FusionAnalytics> result = analyticsService.getFusionAnalytics();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/correlation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CorrelationAnalytics> getCorrelationAnalytics() {
        CorrelationAnalytics result = analyticsService.getCorrelationAnalytics();
        return ResponseEntity.ok(result);
    }
}
