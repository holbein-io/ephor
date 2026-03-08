package io.holbein.ephor.api.controller;

import io.holbein.ephor.api.dto.*;
import io.holbein.ephor.api.dto.vulnerability.*;
import io.holbein.ephor.api.entity.Comment;
import io.holbein.ephor.api.exception.BusinessLogicException;
import io.holbein.ephor.api.exception.ResourceNotFoundException;
import io.holbein.ephor.api.service.VulnerabilitiesService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/vulnerabilities")
@RequiredArgsConstructor
@Tag(name = "Vulnerabilities", description = "CVE browsing, status changes, comments")
public class VulnerabilitiesController {

    private final VulnerabilitiesService vulnerabilitiesService;

    @GetMapping
    public PaginatedResponse<VulnerabilityWithAffectedWorkloads> getAllVulnerabilities(
            @ModelAttribute VulnerabilityListQuery query) {
        return vulnerabilitiesService.getAllVulnerabilities(query);
    }

    @GetMapping("/{id}")
    public VulnerabilityDetailResponse getVulnerability(@PathVariable long id) {
        return vulnerabilitiesService.getVulnerabilityDetail(id);
    }

    @PatchMapping("/{id}/status")
    public Map<String, String> updateStatus(
            @PathVariable long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        int updateCount = vulnerabilitiesService.updateVulnerabilityStatus(id, request.getStatus(), request.isApplyToAll());
        if (updateCount <= 0) {
            throw ResourceNotFoundException.vulnerability(id);
        }
        return Map.of("message", updateCount + " vulnerabilities status updated to " + request.getStatus());
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> getComments(@PathVariable long id) {
        return vulnerabilitiesService.getCommentsByVulnerabilityId(id)
                .stream()
                .map(CommentResponse::fromEntity)
                .toList();
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addComment(
            @PathVariable long id,
            @Valid @RequestBody CommentRequest request,
            @RequestHeader(value = "X-Forwarded-User", required = false) String forwardedUser,
            @RequestHeader(value = "X-Forwarded-Preferred-Username", required = false) String preferredUsername) {

        String defaultAuthor = preferredUsername != null ? preferredUsername :
                (forwardedUser != null ? forwardedUser : "anonymous");

        Comment comment = vulnerabilitiesService.addComment(id, request, defaultAuthor);
        if (comment == null) {
            throw ResourceNotFoundException.vulnerability(id);
        }
        return Map.of("id", comment.getId(), "message", "Comment added successfully");
    }

    @DeleteMapping("/{vulnerabilityId}/comments/{commentId}")
    public Map<String, String> deleteComment(
            @PathVariable long vulnerabilityId,
            @PathVariable long commentId) {
        boolean success = vulnerabilitiesService.deleteComment(commentId, vulnerabilityId);
        if (!success) {
            throw ResourceNotFoundException.comment(commentId, vulnerabilityId);
        }
        return Map.of("message", "Comment deleted successfully");
    }

    @GetMapping("/{id}/triage-info")
    public ResponseEntity<TriageInfoResponse> getTriageInfo(@PathVariable long id) {
        TriageInfoResponse triageInfo = vulnerabilitiesService.getTriageInfo(id);
        if (triageInfo == null) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(triageInfo);
    }

    @PostMapping("/auto-resolve")
    public AutoResolveResponse autoResolve(@RequestBody(required = false) AutoResolveRequest request) {
        if (request == null) {
            request = new AutoResolveRequest();
        }
        int gracePeriodDays = request.getGracePeriodDays() != null ? request.getGracePeriodDays() : 30;
        boolean dryRun = request.getDryRun() != null && request.getDryRun();
        return vulnerabilitiesService.autoResolveVulnerabilities(gracePeriodDays, dryRun);
    }

    @PostMapping("/{id}/reopen")
    public Map<String, String> reopenVulnerability(@PathVariable long id) {
        boolean success = vulnerabilitiesService.reopenAutoResolvedVulnerability(id);
        if (!success) {
            throw BusinessLogicException.cannotReopenVulnerability(id,
                    "Vulnerability is not auto-resolved or does not exist");
        }
        return Map.of("message", "Vulnerability reopened successfully");
    }
}
