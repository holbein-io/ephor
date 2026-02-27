package io.holbein.ephor.api.dto.remediation;

import io.holbein.ephor.api.model.enums.RemediationPriority;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Request to update remediation fields (not status)")
public record UpdateRemediationRequest(

        @Schema(description = "Person assigned to remediate", example = "bob")
        String assignedTo,

        @Schema(description = "Target date for remediation completion", example = "2026-03-15")
        LocalDate targetDate,

        @Schema(description = "Remediation priority", example = "high")
        RemediationPriority priority,

        @Schema(description = "Notes")
        String notes
) {}
