package io.holbein.ephor.api.dto.triage.shared;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Summary of an affected workload")
public record WorkloadSummary(
        long id,
        String namespace,
        String name,
        String kind,
        String imageNames
) {}
