package io.holbein.ephor.api.dto.dashboard;

import java.util.Map;

public record DashboardMetricsResponse(
        long totalVulnerabilities,
        long totalActiveVulnerabilities,
        Map<String, Long> bySeverity,
        Map<String, Long> byStatus,
        long activeEscalations
) {}
