package io.holbein.ephor.api.exception;

/**
 * Exception thrown when a request requires authentication but
 * no valid credentials were provided.
 *
 * Maps to HTTP 401 Unauthorized.
 */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String detail) {
        super(ProblemType.UNAUTHORIZED, detail);
    }

    public UnauthorizedException(String detail, Throwable cause) {
        super(ProblemType.UNAUTHORIZED, detail, cause);
    }
}
