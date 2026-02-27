package io.holbein.ephor.api.mapper.remediation;

import io.holbein.ephor.api.dto.remediation.CreateRemediationRequest;
import io.holbein.ephor.api.dto.remediation.RemediationDetailResponse;
import io.holbein.ephor.api.dto.remediation.RemediationResponse;
import io.holbein.ephor.api.entity.Remediation;
import io.holbein.ephor.api.entity.TriageDecision;
import io.holbein.ephor.api.entity.TriageSession;
import io.holbein.ephor.api.entity.Vulnerability;

import java.time.LocalDate;
import java.util.List;

public final class RemediationMapper {

    private RemediationMapper() {}

    public static Remediation toEntity(CreateRemediationRequest request, Vulnerability vulnerability) {
        return Remediation.builder()
                .vulnerability(vulnerability)
                .assignedTo(request.assignedTo())
                .targetDate(request.targetDate())
                .priority(request.priority())
                .notes(request.notes())
                .build();
    }

    public static RemediationResponse toResponse(Remediation entity) {
        Vulnerability vuln = entity.getVulnerability();
        TriageDecision decision = entity.getTriageDecision();
        TriageSession session = decision != null ? decision.getTriageSession() : null;

        return new RemediationResponse(
                entity.getId(),
                vuln.getId(),
                decision != null ? decision.getId() : null,
                entity.getStatus(),
                entity.getPriority(),
                entity.getAssignedTo(),
                entity.getTargetDate(),
                entity.getNotes(),
                entity.getCompletedAt(),
                entity.getCompletionMethod(),
                entity.getCompletedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                vuln.getCveId(),
                vuln.getPackageName(),
                vuln.getPackageVersion(),
                vuln.getSeverity(),
                vuln.getTitle(),
                vuln.getInstances() != null ? vuln.getInstances().size() : 0,
                session != null ? session.getId() : null,
                session != null ? session.getSessionDate() : null
        );
    }

    public static RemediationDetailResponse toDetailResponse(Remediation entity) {
        Vulnerability vuln = entity.getVulnerability();
        TriageDecision decision = entity.getTriageDecision();
        TriageSession session = decision != null ? decision.getTriageSession() : null;

        List<io.holbein.ephor.api.dto.remediation.RemediationCommentResponse> comments =
                entity.getComments() != null
                        ? entity.getComments().stream()
                                .map(RemediationCommentMapper::toResponse)
                                .toList()
                        : List.of();

        return new RemediationDetailResponse(
                entity.getId(),
                vuln.getId(),
                decision != null ? decision.getId() : null,
                entity.getStatus(),
                entity.getPriority(),
                entity.getAssignedTo(),
                entity.getTargetDate(),
                entity.getNotes(),
                entity.getCompletedAt(),
                entity.getCompletionMethod(),
                entity.getCompletedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                vuln.getCveId(),
                vuln.getPackageName(),
                vuln.getPackageVersion(),
                vuln.getSeverity(),
                vuln.getTitle(),
                vuln.getDescription(),
                vuln.getPrimaryUrl(),
                vuln.getFixedVersion(),
                vuln.getInstances() != null ? vuln.getInstances().size() : 0,
                session != null ? session.getId() : null,
                session != null ? session.getSessionDate() : null,
                comments
        );
    }
}
