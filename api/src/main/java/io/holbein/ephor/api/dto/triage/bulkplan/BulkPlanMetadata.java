package io.holbein.ephor.api.dto.triage.bulkplan;

import io.holbein.ephor.api.model.enums.RemediationPriority;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Additional metadata for bulk plan actions, primarily for needs_remediation")
public record BulkPlanMetadata(

        @Schema(description = "Person assigned to remediate", example = "bob")
        String assignedTo,

        @Schema(description = "Target date for remediation", example = "2026-03-15")
        LocalDate targetDate,

        @Schema(description = "Remediation priority", example = "high")
        RemediationPriority priority,

        @Schema(description = "Reason for the bulk action")
        String reason
) {}
