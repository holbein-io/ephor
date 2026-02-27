package io.holbein.ephor.api.exception;

import org.springframework.http.HttpStatus;

/**
 * RFC 7807 Problem Types for the Ephor API.
 * Each type represents a specific error condition with a unique URI, title, and default HTTP status.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7807">RFC 7807 - Problem Details for HTTP APIs</a>
 */
public enum ProblemType {

    // Resource Not Found (404)
    VULNERABILITY_NOT_FOUND("/problems/vulnerability-not-found", "Vulnerability Not Found", HttpStatus.NOT_FOUND),
    WORKLOAD_NOT_FOUND("/problems/workload-not-found", "Workload Not Found", HttpStatus.NOT_FOUND),
    SCAN_NOT_FOUND("/problems/scan-not-found", "Scan Not Found", HttpStatus.NOT_FOUND),
    COMMENT_NOT_FOUND("/problems/comment-not-found", "Comment Not Found", HttpStatus.NOT_FOUND),
    TRIAGE_SESSION_NOT_FOUND("/problems/triage-session-not-found", "Triage Session Not Found", HttpStatus.NOT_FOUND),
    TRIAGE_DECISION_NOT_FOUND("/problems/triage-decision-not-found", "Triage Decision Not Found", HttpStatus.NOT_FOUND),
    REMEDIATION_NOT_FOUND("/problems/remediation-not-found", "Remediation Not Found", HttpStatus.NOT_FOUND),
    ESCALATION_NOT_FOUND("/problems/escalation-not-found", "Escalation Not Found", HttpStatus.NOT_FOUND),
    RESOURCE_NOT_FOUND("/problems/resource-not-found", "Resource Not Found", HttpStatus.NOT_FOUND),

    // Validation Errors (400)
    VALIDATION_ERROR("/problems/validation-error", "Validation Error", HttpStatus.BAD_REQUEST),
    INVALID_QUERY_PARAMETER("/problems/invalid-query-parameter", "Invalid Query Parameter", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELD("/problems/missing-required-field", "Missing Required Field", HttpStatus.BAD_REQUEST),
    INVALID_FORMAT("/problems/invalid-format", "Invalid Format", HttpStatus.BAD_REQUEST),
    INVALID_SEVERITY("/problems/invalid-severity", "Invalid Severity Level", HttpStatus.BAD_REQUEST),
    INVALID_STATUS("/problems/invalid-status", "Invalid Status Value", HttpStatus.BAD_REQUEST),

    // Business Logic Errors (409/422)
    DUPLICATE_RESOURCE("/problems/duplicate-resource", "Duplicate Resource", HttpStatus.CONFLICT),
    DUPLICATE_VULNERABILITY("/problems/duplicate-vulnerability", "Duplicate Vulnerability", HttpStatus.CONFLICT),
    DUPLICATE_WORKLOAD("/problems/duplicate-workload", "Duplicate Workload", HttpStatus.CONFLICT),

    INVALID_STATE_TRANSITION("/problems/invalid-state-transition", "Invalid State Transition", HttpStatus.UNPROCESSABLE_ENTITY),
    INVALID_STATUS_TRANSITION("/problems/invalid-status-transition", "Invalid Status Transition", HttpStatus.UNPROCESSABLE_ENTITY),
    TRIAGE_SESSION_NOT_ACTIVE("/problems/triage-session-not-active", "Triage Session Not Active", HttpStatus.UNPROCESSABLE_ENTITY),
    TRIAGE_SESSION_ALREADY_COMPLETED("/problems/triage-session-already-completed", "Triage Session Already Completed", HttpStatus.UNPROCESSABLE_ENTITY),
    VULNERABILITY_ALREADY_RESOLVED("/problems/vulnerability-already-resolved", "Vulnerability Already Resolved", HttpStatus.UNPROCESSABLE_ENTITY),
    CANNOT_REOPEN_VULNERABILITY("/problems/cannot-reopen-vulnerability", "Cannot Reopen Vulnerability", HttpStatus.UNPROCESSABLE_ENTITY),
    CANNOT_DELETE_WITH_DEPENDENCIES("/problems/cannot-delete-with-dependencies", "Cannot Delete Resource With Dependencies", HttpStatus.UNPROCESSABLE_ENTITY),

    // Authentication & Authorization (401/403)
    UNAUTHORIZED("/problems/unauthorized", "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("/problems/forbidden", "Forbidden", HttpStatus.FORBIDDEN),
    INSUFFICIENT_PERMISSIONS("/problems/insufficient-permissions", "Insufficient Permissions", HttpStatus.FORBIDDEN),

    // Server Errors (5xx)
    INTERNAL_ERROR("/problems/internal-error", "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_ERROR("/problems/database-error", "Database Error", HttpStatus.INTERNAL_SERVER_ERROR),
    SCAN_INGESTION_FAILED("/problems/scan-ingestion-failed", "Scan Ingestion Failed", HttpStatus.INTERNAL_SERVER_ERROR),
    EXTERNAL_SERVICE_ERROR("/problems/external-service-error", "External Service Error", HttpStatus.BAD_GATEWAY);

    private final String type;
    private final String title;
    private final HttpStatus status;

    ProblemType(String type, String title, HttpStatus status) {
        this.type = type;
        this.title = title;
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public int getStatusCode() {
        return status.value();
    }
}
