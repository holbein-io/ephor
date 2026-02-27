package io.holbein.ephor.api.exception;

import lombok.Getter;

import java.util.Collections;
import java.util.Map;

/**
 * Base exception class for all API errors.
 * Carries the ProblemType and optional extensions for RFC 7807 responses.
 */
@Getter
public class ApiException extends RuntimeException {

    private final ProblemType problemType;
    private final Map<String, Object> extensions;

    public ApiException(ProblemType problemType, String detail) {
        super(detail);
        this.problemType = problemType;
        this.extensions = Collections.emptyMap();
    }

    public ApiException(ProblemType problemType, String detail, Throwable cause) {
        super(detail, cause);
        this.problemType = problemType;
        this.extensions = Collections.emptyMap();
    }

    public ApiException(ProblemType problemType, String detail, Map<String, Object> extensions) {
        super(detail);
        this.problemType = problemType;
        this.extensions = extensions != null ? extensions : Collections.emptyMap();
    }

    public ApiException(ProblemType problemType, String detail, Map<String, Object> extensions, Throwable cause) {
        super(detail, cause);
        this.problemType = problemType;
        this.extensions = extensions != null ? extensions : Collections.emptyMap();
    }
}
