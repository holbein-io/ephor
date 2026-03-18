package io.holbein.ephor.api.filter;

import io.holbein.ephor.api.auth.UserContext;
import io.holbein.ephor.api.auth.UserContextHolder;
import io.holbein.ephor.api.service.UserRegistryService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Filter that extracts user information from OAuth2-proxy headers.
 *
 * OAuth2-proxy (when configured with --pass-user-headers=true) forwards
 * authenticated user information via the following headers:
 *
 * - X-Forwarded-User: The authenticated username
 * - X-Forwarded-Email: The user's email address
 * - X-Forwarded-Groups: Comma-separated list of groups
 * - X-Forwarded-Preferred-Username: Display name (if available)
 * - X-Forwarded-Access-Token: The OAuth access token (optional)
 *
 * This filter runs early in the chain to populate UserContextHolder
 * before any controllers or services need user information.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // Run after TraceIdFilter
public class OAuth2ProxyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(OAuth2ProxyAuthFilter.class);

    private final UserRegistryService userRegistryService;

    public OAuth2ProxyAuthFilter(UserRegistryService userRegistryService) {
        this.userRegistryService = userRegistryService;
    }

    // OAuth2-proxy header names
    public static final String HEADER_USER = "X-Forwarded-User";
    public static final String HEADER_EMAIL = "X-Forwarded-Email";
    public static final String HEADER_GROUPS = "X-Forwarded-Groups";
    public static final String HEADER_PREFERRED_USERNAME = "X-Forwarded-Preferred-Username";
    public static final String HEADER_ACCESS_TOKEN = "X-Forwarded-Access-Token";

    // MDC key for logging
    private static final String MDC_USER_KEY = "user";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            UserContext userContext = extractUserContext(request);

            if (userContext != null) {
                UserContextHolder.setContext(userContext);
                MDC.put(MDC_USER_KEY, userContext.username());

                if (log.isDebugEnabled()) {
                    log.debug("Authenticated user: {} ({}), groups: {}",
                            userContext.username(),
                            userContext.email(),
                            userContext.groups());
                }
            }

            // Register/update user in lazy registry (non-blocking)
            if (userContext != null) {
                try {
                    userRegistryService.registerOrUpdate(userContext);
                } catch (Exception e) {
                    log.debug("User registry update failed (non-blocking)", e);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clearContext();
            MDC.remove(MDC_USER_KEY);
        }
    }

    private UserContext extractUserContext(HttpServletRequest request) {
        String rawUser = getHeader(request, HEADER_USER);

        // No user header means not authenticated via oauth2-proxy
        if (rawUser == null) {
            return null;
        }

        String email = getHeader(request, HEADER_EMAIL);
        String groupsHeader = getHeader(request, HEADER_GROUPS);
        String preferredUsername = getHeader(request, HEADER_PREFERRED_USERNAME);
        String accessToken = getHeader(request, HEADER_ACCESS_TOKEN);

        // Prefer preferred_username over X-Forwarded-User (which may be a UUID from Keycloak)
        String username = preferredUsername != null ? preferredUsername : rawUser;

        List<String> groups = parseGroups(groupsHeader);

        return new UserContext(username, email, groups, preferredUsername, accessToken);
    }

    private String getHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return null;
    }

    private List<String> parseGroups(String groupsHeader) {
        if (groupsHeader == null || groupsHeader.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(groupsHeader.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                // Strip org prefix for GitHub OAuth2 (e.g. "holbein-io:ephor-analysts" -> "ephor-analysts")
                .map(g -> g.contains(":") ? g.substring(g.indexOf(":") + 1) : g)
                .toList();
    }
}
