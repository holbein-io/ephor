package io.holbein.ephor.api.exception;

/**
 * Exception thrown when an authenticated user lacks the required
 * permissions to access a resource or perform an action.
 *
 * Maps to HTTP 403 Forbidden.
 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String detail) {
        super(ProblemType.FORBIDDEN, detail);
    }

    public ForbiddenException(String detail, Throwable cause) {
        super(ProblemType.FORBIDDEN, detail, cause);
    }
}
