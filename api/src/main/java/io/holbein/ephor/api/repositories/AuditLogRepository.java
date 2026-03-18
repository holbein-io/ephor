package io.holbein.ephor.api.repositories;

import io.holbein.ephor.api.entity.AuditLog;
import io.holbein.ephor.api.model.enums.AuditAction;
import io.holbein.ephor.api.model.enums.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(EntityType entityType, Long entityId);

    List<AuditLog> findByPerformedByOrderByCreatedAtDesc(String performedBy);

    List<AuditLog> findByActionOrderByCreatedAtDesc(AuditAction action);

    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);

    long deleteByCreatedAtBefore(Instant cutoff);
}
