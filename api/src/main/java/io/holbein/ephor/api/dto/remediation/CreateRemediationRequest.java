package io.holbein.ephor.api.dto.remediation;

import io.holbein.ephor.api.model.enums.RemediationPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Request to create a standalone remediation (outside triage workflow)")
public record CreateRemediationRequest(

        @Schema(description = "ID of the vulnerability to remediate", example = "42")
        @NotNull
        Long vulnerabilityId,

        @Schema(description = "Person assigned to remediate", example = "alice")
        String assignedTo,

        @Schema(description = "Target date for remediation completion", example = "2026-03-15")
        LocalDate targetDate,

        @Schema(description = "Remediation priority", example = "high")
        RemediationPriority priority,

        @Schema(description = "Initial notes")
        String notes
) {}
