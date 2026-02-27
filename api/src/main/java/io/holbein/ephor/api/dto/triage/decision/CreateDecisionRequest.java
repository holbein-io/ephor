package io.holbein.ephor.api.dto.triage.decision;

import io.holbein.ephor.api.model.enums.DecisionStatus;
import io.holbein.ephor.api.model.enums.RemediationPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Request to create a triage decision on a vulnerability")
public record CreateDecisionRequest(

        @Schema(description = "ID of the triage session", example = "1")
        @NotNull
        Long sessionId,

        @Schema(description = "ID of the vulnerability being decided on", example = "42")
        @NotNull
        Long vulnerabilityId,

        @Schema(description = "Decision status", example = "needs_remediation")
        @NotNull
        DecisionStatus status,

        @Schema(description = "Decision notes")
        String notes,

        @Schema(description = "Username of the person making the decision", example = "alice")
        @NotNull
        String decidedBy,

        @Schema(description = "Person assigned to remediate (only for needs_remediation)")
        String assignedTo,

        @Schema(description = "Target date for remediation (only for needs_remediation)", example = "2026-03-15")
        LocalDate targetDate,

        @Schema(description = "Remediation priority (only for needs_remediation)", example = "high")
        RemediationPriority priority,

        @Schema(description = "ID of the original vulnerability (only for duplicate)")
        Long duplicateOfVulnerabilityId
) {}
