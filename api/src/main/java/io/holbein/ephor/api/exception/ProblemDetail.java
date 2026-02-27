package io.holbein.ephor.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * RFC 7807 Problem Details response object.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7807">RFC 7807 - Problem Details for HTTP APIs</a>
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "title", "status", "detail", "instance", "traceId", "timestamp", "errors", "extensions"})
public class ProblemDetail {

    /**
     * A URI reference that identifies the problem type.
     * When dereferenced, it should provide human-readable documentation.
     */
    private String type;

    /**
     * A short, human-readable summary of the problem type.
     * It should not change from occurrence to occurrence.
     */
    private String title;

    /**
     * The HTTP status code for this occurrence of the problem.
     */
    private int status;

    /**
     * A human-readable explanation specific to this occurrence of the problem.
     */
    private String detail;

    /**
     * A URI reference that identifies the specific occurrence of the problem.
     * Typically the request URI that caused the error.
     */
    private String instance;

    /**
     * Unique identifier for tracing this request across services.
     */
    private String traceId;

    /**
     * Timestamp when the error occurred.
     */
    private Instant timestamp;

    /**
     * List of field-level validation errors.
     * Only populated for validation errors.
     */
    private List<FieldError> errors;

    /**
     * Additional context-specific properties.
     */
    private Map<String, Object> extensions;

    /**
     * Represents a field-level validation error.
     */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }

    /**
     * Creates a ProblemDetail from a ProblemType with a specific detail message.
     */
    public static ProblemDetail of(ProblemType problemType, String detail, String instance, String traceId) {
        return ProblemDetail.builder()
                .type(problemType.getType())
                .title(problemType.getTitle())
                .status(problemType.getStatusCode())
                .detail(detail)
                .instance(instance)
                .traceId(traceId)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates a ProblemDetail for validation errors with field-level details.
     */
    public static ProblemDetail ofValidation(String detail, String instance, String traceId, List<FieldError> errors) {
        return ProblemDetail.builder()
                .type(ProblemType.VALIDATION_ERROR.getType())
                .title(ProblemType.VALIDATION_ERROR.getTitle())
                .status(ProblemType.VALIDATION_ERROR.getStatusCode())
                .detail(detail)
                .instance(instance)
                .traceId(traceId)
                .timestamp(Instant.now())
                .errors(errors)
                .build();
    }
}
