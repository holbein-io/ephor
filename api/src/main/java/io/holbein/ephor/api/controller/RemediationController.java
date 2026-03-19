package io.holbein.ephor.api.controller;

import io.holbein.ephor.api.dto.remediation.*;
import io.holbein.ephor.api.model.enums.RemediationPriority;
import io.holbein.ephor.api.service.RemediationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/remediations")
@RequiredArgsConstructor
@Tag(name = "Remediations", description = "Remediation lifecycle and comments")
public class RemediationController {

    private final RemediationService remediationService;

    @GetMapping("/vulnerability/{vulnerabilityId}")
    public List<RemediationDetailResponse> getRemediationsByVulnerability(@PathVariable Long vulnerabilityId) {
        return remediationService.getRemediationsByVulnerability(vulnerabilityId);
    }

    @GetMapping("/statistics")
    public RemediationStatisticsResponse getStatistics() {
        return remediationService.getStatistics();
    }

    @GetMapping("/overdue")
    public List<RemediationResponse> getOverdueRemediations(
            @RequestParam(required = false) RemediationPriority priority,
            @RequestParam(required = false, name = "assigned_to") String assignedTo) {
        return remediationService.getOverdueRemediations(priority, assignedTo);
    }

    @GetMapping("/{id}")
    public RemediationDetailResponse getRemediation(@PathVariable Long id) {
        return remediationService.getRemediation(id);
    }

    @PostMapping
    public RemediationResponse createRemediation(@Valid @RequestBody CreateRemediationRequest request) {
        return remediationService.createRemediation(request);
    }

    @PatchMapping("/{id}")
    public RemediationResponse updateRemediation(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateRemediationRequest request) {
        return remediationService.updateRemediation(id, request);
    }

    @PatchMapping("/{id}/status")
    public RemediationResponse changeRemediationStatus(@PathVariable Long id,
                                                        @Valid @RequestBody ChangeRemediationStatusRequest request) {
        return remediationService.changeRemediationStatus(id, request);
    }

    @GetMapping("/{id}/comments")
    public List<RemediationCommentResponse> getComments(@PathVariable Long id) {
        return remediationService.getComments(id);
    }

    @PostMapping("/{id}/comments")
    public RemediationCommentResponse addComment(@PathVariable Long id,
                                                  @Valid @RequestBody AddRemediationCommentRequest request) {
        return remediationService.addComment(id, request);
    }
}
