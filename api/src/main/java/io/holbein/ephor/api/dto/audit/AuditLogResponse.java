package io.holbein.ephor.api.dto.audit;

import io.holbein.ephor.api.entity.AuditLog;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Audit log entry")
public record AuditLogResponse(
        Long id,
        String action,
        String entityType,
        Long entityId,
        String performedBy,
        String details,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog entry) {
        return new AuditLogResponse(
                entry.getId(),
                entry.getAction().name(),
                entry.getEntityType().name(),
                entry.getEntityId(),
                entry.getPerformedBy(),
                entry.getDetails(),
                entry.getCreatedAt()
        );
    }
}
