package io.holbein.ephor.api.dto.triage.bulkplan;

import io.holbein.ephor.api.model.enums.BulkPlanStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Result of bulk plan execution")
public record ExecuteBulkPlanResponse(
        long planId,

        @Schema(description = "Always 'executed' after successful execution")
        BulkPlanStatus status,

        Instant executedAt,
        String executedBy,

        @Schema(description = "Original estimated count at plan creation time")
        int estimatedCount,

        @Schema(description = "Actual number of vulnerabilities affected")
        int actualCount,

        @Schema(description = "Number of vulnerabilities skipped (already decided individually)")
        int skippedCount,

        @Schema(description = "List of decisions created by this execution")
        List<CreatedDecision> createdDecisions
) {}
