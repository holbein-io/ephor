package io.holbein.ephor.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.holbein.ephor.api.auth.UserContextHolder;
import io.holbein.ephor.api.entity.AuditLog;
import io.holbein.ephor.api.model.enums.AuditAction;
import io.holbein.ephor.api.model.enums.EntityType;
import io.holbein.ephor.api.repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void log(AuditAction action, EntityType entityType, Long entityId, Object details) {
        String actor = UserContextHolder.getUsername("system");
        String detailsJson = serializeDetails(details);

        AuditLog entry = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .performedBy(actor)
                .details(detailsJson)
                .build();

        auditLogRepository.save(entry);
        log.debug("Audit: {} on {}#{} by {}", action, entityType, entityId, actor);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getByEntity(EntityType entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                entityType, entityId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getByActor(String actor) {
        return auditLogRepository.findByPerformedByOrderByCreatedAtDesc(actor);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getByAction(AuditAction action) {
        return auditLogRepository.findByActionOrderByCreatedAtDesc(action);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getByDateRange(Instant from, Instant to) {
        return auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to);
    }

    private String serializeDetails(Object details) {
        if (details == null) {
            return null;
        }
        if (details instanceof String s) {
            return s;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            log.warn("Failed to serialize audit details", e);
            return details.toString();
        }
    }
}
