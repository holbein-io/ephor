package io.holbein.ephor.api.mapper.escalation;

import io.holbein.ephor.api.dto.escalation.CreateEscalationRequest;
import io.holbein.ephor.api.dto.escalation.EscalationResponse;
import io.holbein.ephor.api.entity.Escalation;
import io.holbein.ephor.api.entity.Vulnerability;
import io.holbein.ephor.api.model.enums.EscalationStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class EscalationMapper {

    private EscalationMapper() {}

    public static Escalation toEntity(CreateEscalationRequest request, Vulnerability vulnerability) {
        Instant now = Instant.now();
        return Escalation.builder()
                .vulnerability(vulnerability)
                .escalationLevel(request.escalationLevel())
                .escalatedAt(now)
                .escalatedBy(request.escalatedBy())
                .reason(request.reason())
                .status(EscalationStatus.pending)
                .dueDate(now.plus(5, ChronoUnit.DAYS))
                .build();
    }

    public static EscalationResponse toResponse(Escalation entity) {
        Vulnerability v = entity.getVulnerability();
        return new EscalationResponse(
                entity.getId(),
                v != null ? v.getId() : null,
                entity.getEscalationLevel(),
                entity.getEscalatedAt(),
                entity.getEscalatedBy(),
                entity.getReason(),
                entity.getStatus(),
                entity.getPriority(),
                entity.getDueDate(),
                entity.getMsTeamsMessageId(),
                v != null ? v.getCveId() : null,
                v != null && v.getSeverity() != null ? v.getSeverity().name() : null
        );
    }
}
