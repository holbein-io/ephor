package io.holbein.ephor.api.dto.triage.decision;

import io.holbein.ephor.api.model.enums.DecisionStatus;
import io.holbein.ephor.api.model.enums.RemediationPriority;
import io.holbein.ephor.api.model.enums.RemediationStatus;
import io.holbein.ephor.api.model.enums.SeverityLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "Triage decision details with denormalized vulnerability data")
public record DecisionResponse(
        long id,
        long sessionId,
        long vulnerabilityId,
        DecisionStatus status,
        String notes,
        String decidedBy,
        Instant decidedAt,
        String assignedTo,
        LocalDate targetDate,
        RemediationPriority priority,
        Long duplicateOfVulnerabilityId,

        // Denormalized from Vulnerability
        String cveId,
        String packageName,
        String packageVersion,
        SeverityLevel severity,
        String title,
        int affectedWorkloads,

        // Remediation reference (only for needs_remediation)
        @Schema(description = "Remediation ID, present when status is needs_remediation")
        Long remediationId,

        @Schema(description = "Remediation status, present when status is needs_remediation")
        RemediationStatus remediationStatus
) {}
