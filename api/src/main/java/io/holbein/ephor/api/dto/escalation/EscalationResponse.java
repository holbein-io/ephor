package io.holbein.ephor.api.dto.escalation;

import io.holbein.ephor.api.model.enums.EscalationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Escalation details")
public record EscalationResponse(
        Long id,
        Long vulnerabilityId,
        Integer escalationLevel,
        Instant escalatedAt,
        String escalatedBy,
        String reason,
        EscalationStatus status,
        String priority,
        Instant dueDate,
        String msTeamsMessageId,
        @Schema(description = "CVE ID of the escalated vulnerability")
        String cveId,
        @Schema(description = "Severity of the escalated vulnerability")
        String severity
) {}
