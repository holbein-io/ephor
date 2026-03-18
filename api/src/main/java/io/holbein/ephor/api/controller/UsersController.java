package io.holbein.ephor.api.controller;

import io.holbein.ephor.api.auth.RequireAuth;
import io.holbein.ephor.api.auth.UserContextHolder;
import io.holbein.ephor.api.directory.UserDirectoryProvider;
import io.holbein.ephor.api.dto.user.MyItemsResponse;
import io.holbein.ephor.api.dto.user.UserDirectoryCapabilities;
import io.holbein.ephor.api.entity.Comment;
import io.holbein.ephor.api.entity.Escalation;
import io.holbein.ephor.api.entity.KnownUser;
import io.holbein.ephor.api.exception.ProblemType;
import io.holbein.ephor.api.exception.ResourceNotFoundException;
import io.holbein.ephor.api.model.enums.Permission;
import io.holbein.ephor.api.repositories.CommentRepository;
import io.holbein.ephor.api.repositories.EscalationRepository;
import io.holbein.ephor.api.repositories.KnownUserRepository;
import io.holbein.ephor.api.repositories.RemediationRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User directory and management")
public class UsersController {

    private final KnownUserRepository knownUserRepository;
    private final UserDirectoryProvider userDirectoryProvider;
    private final EscalationRepository escalationRepository;
    private final CommentRepository commentRepository;
    private final RemediationRepository remediationRepository;

    @GetMapping
    @RequireAuth(permissions = {Permission.VIEW_ADMIN})
    public ResponseEntity<List<KnownUser>> listUsers() {
        return ResponseEntity.ok(knownUserRepository.findAll());
    }

    @GetMapping("/{username}")
    @RequireAuth(permissions = {Permission.VIEW_ADMIN})
    public ResponseEntity<KnownUser> getUser(@PathVariable String username) {
        return knownUserRepository.findById(username)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ProblemType.RESOURCE_NOT_FOUND,
                        String.format("User '%s' not found", username)
                ));
    }

    @GetMapping("/capabilities")
    public ResponseEntity<UserDirectoryCapabilities> getCapabilities() {
        return ResponseEntity.ok(userDirectoryProvider.getCapabilities());
    }

    @GetMapping("/search")
    @RequireAuth
    public ResponseEntity<List<KnownUser>> searchUsers(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(userDirectoryProvider.searchUsers(query, Math.min(limit, 50)));
    }

    @GetMapping("/me/items")
    @RequireAuth
    @Transactional(readOnly = true)
    public ResponseEntity<MyItemsResponse> getMyItems() {
        String username = UserContextHolder.getUsername("anonymous");

        List<MyItemsResponse.MyEscalation> myEscalations = escalationRepository
                .findByEscalatedByOrderByEscalatedAtDesc(username).stream()
                .map(e -> new MyItemsResponse.MyEscalation(
                        e.getId(),
                        e.getVulnerability() != null ? e.getVulnerability().getId() : null,
                        e.getVulnerability() != null ? e.getVulnerability().getCveId() : null,
                        e.getStatus() != null ? e.getStatus().name() : null,
                        e.getEscalatedAt() != null ? e.getEscalatedAt().toString() : null))
                .toList();

        List<MyItemsResponse.MyComment> myComments = commentRepository
                .findByCreatedByOrderByCreatedAtDesc(username).stream()
                .limit(20)
                .map(c -> {
                    Long vulnId = c.getVulnerability() != null ? c.getVulnerability().getId() : c.getEntityId();
                    String cveId = c.getVulnerability() != null ? c.getVulnerability().getCveId() : null;
                    String entityType = c.getEntityType() != null ? c.getEntityType().name()
                            : (c.getVulnerability() != null ? "VULNERABILITY" : null);
                    return new MyItemsResponse.MyComment(
                            c.getId(),
                            entityType,
                            c.getEntityId(),
                            vulnId,
                            cveId,
                            c.getBody(),
                            c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
                })
                .toList();

        List<MyItemsResponse.MyRemediation> myRemediations = remediationRepository
                .findByAssignedToOrderByTargetDateAsc(username).stream()
                .map(r -> new MyItemsResponse.MyRemediation(
                        r.getId(),
                        r.getVulnerability() != null ? r.getVulnerability().getId() : null,
                        r.getVulnerability() != null ? r.getVulnerability().getCveId() : null,
                        r.getStatus() != null ? r.getStatus().name() : null,
                        r.getTargetDate() != null ? r.getTargetDate().toString() : null))
                .toList();

        return ResponseEntity.ok(new MyItemsResponse(myRemediations, myEscalations, myComments));
    }
}
