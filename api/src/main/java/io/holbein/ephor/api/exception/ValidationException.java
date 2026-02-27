package io.holbein.ephor.api.exception;

import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Exception thrown when request validation fails.
 * Contains a list of field-level errors.
 */
@Getter
public class ValidationException extends ApiException {

    private final List<ProblemDetail.FieldError> fieldErrors;

    public ValidationException(String detail, List<ProblemDetail.FieldError> fieldErrors) {
        super(ProblemType.VALIDATION_ERROR, detail);
        this.fieldErrors = fieldErrors;
    }

    public ValidationException(ProblemType problemType, String detail, List<ProblemDetail.FieldError> fieldErrors) {
        super(problemType, detail);
        this.fieldErrors = fieldErrors;
    }

    // Convenience factory methods

    public static ValidationException singleField(String field, String message) {
        return new ValidationException(
                String.format("Validation failed for field '%s'", field),
                List.of(ProblemDetail.FieldError.builder()
                        .field(field)
                        .message(message)
                        .build())
        );
    }

    public static ValidationException singleField(String field, String message, Object rejectedValue) {
        return new ValidationException(
                String.format("Validation failed for field '%s'", field),
                List.of(ProblemDetail.FieldError.builder()
                        .field(field)
                        .message(message)
                        .rejectedValue(rejectedValue)
                        .build())
        );
    }

    public static ValidationException multipleFields(String detail, List<ProblemDetail.FieldError> errors) {
        return new ValidationException(detail, errors);
    }

    public static ValidationException invalidQueryParameter(String param, String message) {
        return new ValidationException(
                ProblemType.INVALID_QUERY_PARAMETER,
                String.format("Invalid query parameter '%s': %s", param, message),
                List.of(ProblemDetail.FieldError.builder()
                        .field(param)
                        .message(message)
                        .build())
        );
    }

    public static ValidationException missingRequiredField(String field) {
        return new ValidationException(
                ProblemType.MISSING_REQUIRED_FIELD,
                String.format("Required field '%s' is missing", field),
                List.of(ProblemDetail.FieldError.builder()
                        .field(field)
                        .message("This field is required")
                        .build())
        );
    }
}
