package io.holbein.ephor.api.dto.dashboard;

public record NamespaceComparisonResponse(
        String namespace,
        long critical,
        long high,
        long medium,
        long low
) {}
