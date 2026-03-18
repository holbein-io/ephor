package io.holbein.ephor.api.controller;

import io.holbein.ephor.api.auth.RequireAuth;
import io.holbein.ephor.api.dto.audit.AuditLogResponse;
import io.holbein.ephor.api.entity.AuditLog;
import io.holbein.ephor.api.model.enums.AuditAction;
import io.holbein.ephor.api.model.enums.EntityType;
import io.holbein.ephor.api.model.enums.Permission;
import io.holbein.ephor.api.service.AuditService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Audit trail and activity feed")
@RequireAuth(permissions = {Permission.VIEW_ADMIN})
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<List<AuditLogResponse>> getAuditLog(
            @RequestParam(required = false, name = "entity_type") EntityType entityType,
            @RequestParam(required = false, name = "entity_id") Long entityId,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {

        List<AuditLog> entries;

        if (entityType != null && entityId != null) {
            entries = auditService.getByEntity(entityType, entityId);
        } else if (actor != null) {
            entries = auditService.getByActor(actor);
        } else if (action != null) {
            entries = auditService.getByAction(action);
        } else if (from != null && to != null) {
            entries = auditService.getByDateRange(from, to);
        } else {
            entries = auditService.getByDateRange(
                    Instant.now().minusSeconds(86400 * 30), Instant.now());
        }

        List<AuditLogResponse> response = entries.stream()
                .map(AuditLogResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }
}
