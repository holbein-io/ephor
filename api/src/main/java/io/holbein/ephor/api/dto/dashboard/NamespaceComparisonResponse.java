package io.holbein.ephor.api.dto.dashboard;

public record NamespaceComparisonResponse(
        String namespace,
        long totalVulnerabilities,
        long critical,
        long high,
        long medium,
        long low,
        long open,
        long resolved
) {}
