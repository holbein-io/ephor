package io.holbein.ephor.api.exception;

import java.util.Map;

/**
 * Exception thrown when a business rule or state transition is violated.
 */
public class BusinessLogicException extends ApiException {

    public BusinessLogicException(ProblemType problemType, String detail) {
        super(problemType, detail);
    }

    public BusinessLogicException(ProblemType problemType, String detail, Map<String, Object> extensions) {
        super(problemType, detail, extensions);
    }

    // Convenience factory methods for common business logic violations

    public static BusinessLogicException invalidStatusTransition(String currentStatus, String targetStatus) {
        return new BusinessLogicException(
                ProblemType.INVALID_STATUS_TRANSITION,
                String.format("Cannot transition from '%s' to '%s'", currentStatus, targetStatus),
                Map.of("currentStatus", currentStatus, "targetStatus", targetStatus)
        );
    }

    public static BusinessLogicException vulnerabilityAlreadyResolved(long vulnerabilityId) {
        return new BusinessLogicException(
                ProblemType.VULNERABILITY_ALREADY_RESOLVED,
                String.format("Vulnerability with ID %d is already resolved", vulnerabilityId),
                Map.of("vulnerabilityId", vulnerabilityId)
        );
    }

    public static BusinessLogicException cannotReopenVulnerability(long vulnerabilityId, String reason) {
        return new BusinessLogicException(
                ProblemType.CANNOT_REOPEN_VULNERABILITY,
                String.format("Cannot reopen vulnerability with ID %d: %s", vulnerabilityId, reason),
                Map.of("vulnerabilityId", vulnerabilityId, "reason", reason)
        );
    }

    public static BusinessLogicException triageSessionNotActive(long sessionId, String currentStatus) {
        return new BusinessLogicException(
                ProblemType.TRIAGE_SESSION_NOT_ACTIVE,
                String.format("Triage session %d is not active (current status: %s)", sessionId, currentStatus),
                Map.of("sessionId", sessionId, "currentStatus", currentStatus)
        );
    }

    public static BusinessLogicException triageSessionAlreadyCompleted(long sessionId) {
        return new BusinessLogicException(
                ProblemType.TRIAGE_SESSION_ALREADY_COMPLETED,
                String.format("Triage session %d has already been completed", sessionId),
                Map.of("sessionId", sessionId)
        );
    }

    public static BusinessLogicException duplicateResource(String resourceType, String identifier) {
        return new BusinessLogicException(
                ProblemType.DUPLICATE_RESOURCE,
                String.format("%s with identifier '%s' already exists", resourceType, identifier),
                Map.of("resourceType", resourceType, "identifier", identifier)
        );
    }

    public static BusinessLogicException cannotDeleteWithDependencies(String resourceType, long id, String dependencyInfo) {
        return new BusinessLogicException(
                ProblemType.CANNOT_DELETE_WITH_DEPENDENCIES,
                String.format("Cannot delete %s with ID %d: %s", resourceType, id, dependencyInfo),
                Map.of("resourceType", resourceType, "resourceId", id, "dependencyInfo", dependencyInfo)
        );
    }
}
