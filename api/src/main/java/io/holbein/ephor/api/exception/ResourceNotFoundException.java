package io.holbein.ephor.api.exception;

import java.util.Map;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(ProblemType problemType, String detail) {
        super(problemType, detail);
    }

    public ResourceNotFoundException(ProblemType problemType, String detail, Map<String, Object> extensions) {
        super(problemType, detail, extensions);
    }

    // Convenience factory methods for common resource types

    public static ResourceNotFoundException vulnerability(long id) {
        return new ResourceNotFoundException(
                ProblemType.VULNERABILITY_NOT_FOUND,
                String.format("Vulnerability with ID %d was not found", id),
                Map.of("vulnerabilityId", id)
        );
    }

    public static ResourceNotFoundException workload(long id) {
        return new ResourceNotFoundException(
                ProblemType.WORKLOAD_NOT_FOUND,
                String.format("Workload with ID %d was not found", id),
                Map.of("workloadId", id)
        );
    }

    public static ResourceNotFoundException scan(long id) {
        return new ResourceNotFoundException(
                ProblemType.SCAN_NOT_FOUND,
                String.format("Scan with ID %d was not found", id),
                Map.of("scanId", id)
        );
    }

    public static ResourceNotFoundException comment(long id) {
        return new ResourceNotFoundException(
                ProblemType.COMMENT_NOT_FOUND,
                String.format("Comment with ID %d was not found", id),
                Map.of("commentId", id)
        );
    }

    public static ResourceNotFoundException comment(long commentId, long vulnerabilityId) {
        return new ResourceNotFoundException(
                ProblemType.COMMENT_NOT_FOUND,
                String.format("Comment with ID %d was not found for vulnerability %d", commentId, vulnerabilityId),
                Map.of("commentId", commentId, "vulnerabilityId", vulnerabilityId)
        );
    }

    public static ResourceNotFoundException triageSession(long id) {
        return new ResourceNotFoundException(
                ProblemType.TRIAGE_SESSION_NOT_FOUND,
                String.format("Triage session with ID %d was not found", id),
                Map.of("sessionId", id)
        );
    }

    public static ResourceNotFoundException triageDecision(long id) {
        return new ResourceNotFoundException(
                ProblemType.TRIAGE_DECISION_NOT_FOUND,
                String.format("Triage decision with ID %d was not found", id),
                Map.of("decisionId", id)
        );
    }

    public static ResourceNotFoundException remediation(long id) {
        return new ResourceNotFoundException(
                ProblemType.REMEDIATION_NOT_FOUND,
                String.format("Remediation with ID %d was not found", id),
                Map.of("remediationId", id)
        );
    }

    public static ResourceNotFoundException escalation(long id) {
        return new ResourceNotFoundException(
                ProblemType.ESCALATION_NOT_FOUND,
                String.format("Escalation with ID %d was not found", id),
                Map.of("escalationId", id)
        );
    }
}
