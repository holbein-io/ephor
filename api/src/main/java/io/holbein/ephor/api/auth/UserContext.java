package io.holbein.ephor.api.auth;

import java.util.List;

/**
 * Represents the authenticated user context extracted from OAuth2-proxy headers.
 *
 * OAuth2-proxy passes user information via X-Forwarded-* headers:
 * - X-Forwarded-User: username
 * - X-Forwarded-Email: email address
 * - X-Forwarded-Groups: comma-separated group list
 * - X-Forwarded-Preferred-Username: display name
 */
public record UserContext(
        String username,
        String email,
        List<String> groups,
        String displayName,
        String accessToken
) {
    /**
     * Returns the display name if available, otherwise falls back to username.
     */
    public String getEffectiveDisplayName() {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        return username;
    }

    /**
     * Checks if the user belongs to any of the specified groups.
     */
    public boolean hasAnyGroup(List<String> allowedGroups) {
        if (groups == null || groups.isEmpty()) {
            return false;
        }
        return groups.stream().anyMatch(allowedGroups::contains);
    }

    /**
     * Checks if the user belongs to all of the specified groups.
     */
    public boolean hasAllGroups(List<String> requiredGroups) {
        if (groups == null || groups.isEmpty()) {
            return false;
        }
        return groups.containsAll(requiredGroups);
    }
}
