package io.holbein.ephor.api.auth;

import java.util.Optional;

/**
 * Thread-local holder for the current user context.
 * Similar pattern to Spring Security's SecurityContextHolder.
 *
 * Usage:
 *   UserContext user = UserContextHolder.getContext().orElseThrow();
 *   String author = UserContextHolder.getDisplayName("Anonymous");
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> contextHolder = new ThreadLocal<>();

    private UserContextHolder() {
    }

    /**
     * Get the current user context.
     */
    public static Optional<UserContext> getContext() {
        return Optional.ofNullable(contextHolder.get());
    }

    /**
     * Get the current user context, throwing if not authenticated.
     */
    public static UserContext requireContext() {
        UserContext context = contextHolder.get();
        if (context == null) {
            throw new IllegalStateException("No user context available - user not authenticated");
        }
        return context;
    }

    /**
     * Set the current user context.
     */
    public static void setContext(UserContext context) {
        contextHolder.set(context);
    }

    /**
     * Clear the current user context.
     */
    public static void clearContext() {
        contextHolder.remove();
    }

    /**
     * Check if a user is currently authenticated.
     */
    public static boolean isAuthenticated() {
        return contextHolder.get() != null;
    }

    /**
     * Get the current user's display name, or a fallback if not authenticated.
     */
    public static String getDisplayName(String fallback) {
        return getContext()
                .map(UserContext::getEffectiveDisplayName)
                .orElse(fallback);
    }

    /**
     * Get the current user's email, or a fallback if not authenticated.
     */
    public static String getEmail(String fallback) {
        return getContext()
                .map(UserContext::email)
                .orElse(fallback);
    }

    /**
     * Get the current user's username, or a fallback if not authenticated.
     */
    public static String getUsername(String fallback) {
        return getContext()
                .map(UserContext::username)
                .orElse(fallback);
    }
}
