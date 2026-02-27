package io.holbein.ephor.api.mapper.triage;

import io.holbein.ephor.api.dto.triage.decision.CreateDecisionRequest;
import io.holbein.ephor.api.dto.triage.decision.DecisionResponse;
import io.holbein.ephor.api.entity.Remediation;
import io.holbein.ephor.api.entity.TriageDecision;
import io.holbein.ephor.api.entity.TriageSession;
import io.holbein.ephor.api.entity.Vulnerability;
import io.holbein.ephor.api.model.enums.DecisionStatus;

public final class DecisionMapper {

    private DecisionMapper() {}

    public static TriageDecision toEntity(CreateDecisionRequest request, TriageSession session, Vulnerability vulnerability) {
        return TriageDecision.builder()
                .triageSession(session)
                .vulnerability(vulnerability)
                .decision(request.status())
                .decidedBy(request.decidedBy())
                .assignedTo(request.assignedTo())
                .targetDate(request.targetDate())
                .notes(request.notes())
                .build();
    }

    public static DecisionResponse toResponse(TriageDecision entity) {
        Vulnerability vuln = entity.getVulnerability();
        Remediation rem = entity.getRemediation();
        boolean hasRemediation = rem != null && entity.getDecision() == DecisionStatus.needs_remediation;

        return new DecisionResponse(
                entity.getId(),
                entity.getTriageSession().getId(),
                vuln.getId(),
                entity.getDecision(),
                entity.getNotes(),
                entity.getDecidedBy(),
                entity.getCreatedAt(),
                entity.getAssignedTo(),
                entity.getTargetDate(),
                hasRemediation ? rem.getPriority() : null,
                null, // duplicateOfVulnerabilityId - not tracked on entity yet
                vuln.getCveId(),
                vuln.getPackageName(),
                vuln.getPackageVersion(),
                vuln.getSeverity(),
                vuln.getTitle(),
                vuln.getInstances() != null ? vuln.getInstances().size() : 0,
                hasRemediation ? rem.getId() : null,
                hasRemediation ? rem.getStatus() : null
        );
    }
}
