package io.holbein.ephor.api.filter;

import io.holbein.ephor.api.auth.UserContext;
import io.holbein.ephor.api.auth.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Development-only filter that injects a mock user when oauth2-proxy is not available.
 *
 * Activated when: auth.dev.enabled=true
 *
 * This allows local development without running oauth2-proxy.
 * NEVER enable this in production!
 *
 * Configuration (application.properties or application-dev.properties):
 *   auth.dev.enabled=true
 *   auth.dev.username=dev-user
 *   auth.dev.email=dev@localhost
 *   auth.dev.groups=developers,security-team
 *   auth.dev.display-name=Development User
 */
@Component
@ConditionalOnProperty(name = "auth.dev.enabled", havingValue = "true")
@Order(Ordered.HIGHEST_PRECEDENCE + 11) // Run after OAuth2ProxyAuthFilter
public class DevUserFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DevUserFilter.class);
    private static final String MDC_USER_KEY = "user";

    private final UserContext devUser;

    public DevUserFilter(
            @Value("${auth.dev.username:dev-user}") String username,
            @Value("${auth.dev.email:dev@localhost}") String email,
            @Value("${auth.dev.groups:developers}") String groups,
            @Value("${auth.dev.display-name:Development User}") String displayName
    ) {
        List<String> groupList = Arrays.stream(groups.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        this.devUser = new UserContext(username, email, groupList, displayName, null);

        log.warn("========================================");
        log.warn("DEV USER MODE ENABLED - DO NOT USE IN PRODUCTION!");
        log.warn("Mock user: {} ({})", username, email);
        log.warn("Groups: {}", groupList);
        log.warn("========================================");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Only inject if no user was set by OAuth2ProxyAuthFilter
        if (!UserContextHolder.isAuthenticated()) {
            UserContextHolder.setContext(devUser);
            MDC.put(MDC_USER_KEY, devUser.username());

            if (log.isDebugEnabled()) {
                log.debug("Injected dev user: {}", devUser.username());
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Only clear if we set it
            if (UserContextHolder.getContext().map(u -> u == devUser).orElse(false)) {
                UserContextHolder.clearContext();
                MDC.remove(MDC_USER_KEY);
            }
        }
    }
}
