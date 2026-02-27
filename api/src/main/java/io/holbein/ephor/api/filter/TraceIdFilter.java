package io.holbein.ephor.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that generates or propagates a trace ID for each request.
 * The trace ID is stored in MDC for logging and included in error responses.
 *
 * Supports propagation from upstream services via common trace headers:
 * - X-Trace-Id
 * - X-Request-Id
 * - X-Correlation-Id
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_KEY = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private static final String[] TRACE_HEADERS = {
            "X-Trace-Id",
            "X-Request-Id",
            "X-Correlation-Id"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = extractOrGenerateTraceId(request);

        try {
            MDC.put(TRACE_ID_KEY, traceId);
            response.setHeader(TRACE_ID_HEADER, traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_KEY);
        }
    }

    private String extractOrGenerateTraceId(HttpServletRequest request) {
        for (String header : TRACE_HEADERS) {
            String traceId = request.getHeader(header);
            if (traceId != null && !traceId.isBlank()) {
                return traceId.trim();
            }
        }
        return generateTraceId();
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
