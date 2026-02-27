package io.holbein.ephor.api.dto.remediation;

import io.holbein.ephor.api.model.enums.CompletionMethod;
import io.holbein.ephor.api.model.enums.RemediationPriority;
import io.holbein.ephor.api.model.enums.RemediationStatus;
import io.holbein.ephor.api.model.enums.SeverityLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "Remediation summary used for lists, create, update, and status change responses")
public record RemediationResponse(
        long id,
        long vulnerabilityId,
        Long triageDecisionId,
        RemediationStatus status,
        RemediationPriority priority,
        String assignedTo,
        LocalDate targetDate,
        String notes,
        Instant completedAt,
        CompletionMethod completionMethod,
        String completedBy,
        Instant createdAt,
        Instant updatedAt,

        @Schema(description = "Denormalized from vulnerability")
        String cveId,
        String packageName,
        String packageVersion,
        SeverityLevel severity,
        String title,
        int affectedWorkloads,

        @Schema(description = "Triage session that created this remediation, if any")
        Long triageSessionId,
        LocalDate triageSessionDate
) {}
