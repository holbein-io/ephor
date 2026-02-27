package io.holbein.ephor.api.controller;

import io.holbein.ephor.api.dto.dashboard.DashboardMetricsResponse;
import io.holbein.ephor.api.dto.dashboard.NamespaceComparisonResponse;
import io.holbein.ephor.api.dto.dashboard.VulnerabilityTrendResponse;
import io.holbein.ephor.api.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Metrics and trend data")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/metrics")
    public DashboardMetricsResponse getMetrics() {
        return dashboardService.getMetrics();
    }

    @GetMapping("/namespaces")
    public List<String> getNamespaces() {
        return dashboardService.getNamespaces();
    }

    @GetMapping("/trends")
    public List<VulnerabilityTrendResponse> getTrends(
            @RequestParam(value = "days", defaultValue = "30") int days) {
        return dashboardService.getTrends(days);
    }

    @GetMapping("/namespace-comparison")
    public List<NamespaceComparisonResponse> getNamespaceComparison() {
        return dashboardService.getNamespaceComparison();
    }
}
