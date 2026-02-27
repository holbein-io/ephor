package io.holbein.ephor.api.exception;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler that converts exceptions to RFC 7807 Problem Details responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TRACE_ID_KEY = "traceId";
    private static final PropertyNamingStrategies.SnakeCaseStrategy SNAKE_CASE = new PropertyNamingStrategies.SnakeCaseStrategy();

    /**
     * Handles all ApiException subclasses.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(ApiException ex, HttpServletRequest request) {
        logException(ex, request);

        ProblemDetail problem = ProblemDetail.builder()
                .type(ex.getProblemType().getType())
                .title(ex.getProblemType().getTitle())
                .status(ex.getProblemType().getStatusCode())
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .traceId(getTraceId())
                .timestamp(java.time.Instant.now())
                .extensions(ex.getExtensions().isEmpty() ? null : ex.getExtensions())
                .build();

        return ResponseEntity.status(ex.getProblemType().getStatus()).body(problem);
    }

    /**
     * Handles ValidationException with field-level errors.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(ValidationException ex, HttpServletRequest request) {
        logException(ex, request);

        ProblemDetail problem = ProblemDetail.builder()
                .type(ex.getProblemType().getType())
                .title(ex.getProblemType().getTitle())
                .status(ex.getProblemType().getStatusCode())
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .traceId(getTraceId())
                .timestamp(java.time.Instant.now())
                .errors(ex.getFieldErrors())
                .build();

        return ResponseEntity.status(ex.getProblemType().getStatus()).body(problem);
    }

    /**
     * Handles Spring's @Valid annotation validation failures.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation failed for request to {}: {}", request.getRequestURI(), ex.getMessage());

        List<ProblemDetail.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> ProblemDetail.FieldError.builder()
                        .field(SNAKE_CASE.translate(error.getField()))
                        .message(error.getDefaultMessage())
                        .rejectedValue(error.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        ProblemDetail problem = ProblemDetail.ofValidation(
                "Request validation failed",
                request.getRequestURI(),
                getTraceId(),
                fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Handles missing request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Missing parameter for request to {}: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetail problem = ProblemDetail.of(
                ProblemType.MISSING_REQUIRED_FIELD,
                String.format("Required parameter '%s' is missing", ex.getParameterName()),
                request.getRequestURI(),
                getTraceId()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Handles type mismatch in request parameters.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Type mismatch for request to {}: {}", request.getRequestURI(), ex.getMessage());

        String detail = String.format("Parameter '%s' should be of type %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        ProblemDetail problem = ProblemDetail.of(
                ProblemType.INVALID_FORMAT,
                detail,
                request.getRequestURI(),
                getTraceId()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Handles unreadable HTTP messages (malformed JSON, etc).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Unreadable message for request to {}: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetail problem = ProblemDetail.of(
                ProblemType.INVALID_FORMAT,
                "Request body is malformed or unreadable",
                request.getRequestURI(),
                getTraceId()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Handles unsupported HTTP methods.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Method not supported for request to {}: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetail problem = ProblemDetail.builder()
                .type("/problems/method-not-allowed")
                .title("Method Not Allowed")
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .detail(String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod()))
                .instance(request.getRequestURI())
                .traceId(getTraceId())
                .timestamp(java.time.Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(problem);
    }

    /**
     * Handles unsupported media types.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.warn("Media type not supported for request to {}: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetail problem = ProblemDetail.builder()
                .type("/problems/unsupported-media-type")
                .title("Unsupported Media Type")
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .detail(String.format("Content type '%s' is not supported", ex.getContentType()))
                .instance(request.getRequestURI())
                .traceId(getTraceId())
                .timestamp(java.time.Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(problem);
    }

    /**
     * Handles 404 errors when no handler is found.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("No handler found for request to {}", request.getRequestURI());

        ProblemDetail problem = ProblemDetail.of(
                ProblemType.RESOURCE_NOT_FOUND,
                String.format("No endpoint found for %s %s", ex.getHttpMethod(), ex.getRequestURL()),
                request.getRequestURI(),
                getTraceId()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    /**
     * Handles all other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error for request to {}", request.getRequestURI(), ex);

        ProblemDetail problem = ProblemDetail.of(
                ProblemType.INTERNAL_ERROR,
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI(),
                getTraceId()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    private void logException(ApiException ex, HttpServletRequest request) {
        if (ex.getProblemType().getStatus().is5xxServerError()) {
            log.error("Server error for request to {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        } else if (ex.getProblemType().getStatus().is4xxClientError()) {
            log.warn("Client error for request to {}: {}", request.getRequestURI(), ex.getMessage());
        }
    }
}
