package io.holbein.ephor.api.dto.triage.bulkplan;

import io.holbein.ephor.api.model.enums.BulkAction;
import io.holbein.ephor.api.model.enums.BulkPlanStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Bulk plan details with matching vulnerability preview")
public record BulkPlanResponse(
        long id,
        long sessionId,
        String name,
        String description,
        BulkAction action,
        BulkPlanFilters filters,
        BulkPlanMetadata metadata,
        BulkPlanStatus status,
        int estimatedCount,
        int actualCount,
        boolean createdDuringPrep,
        String createdBy,
        String executedBy,
        Instant executedAt,
        Instant createdAt,

        @Schema(description = "Preview of matching vulnerabilities (first 50)")
        List<MatchingVulnerability> matchingVulnerabilities
) {}
