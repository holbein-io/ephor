package io.holbein.ephor.api.model.enums;

import java.util.*;

public enum Role {
    VIEWER(Set.of(
            Permission.VIEW_VULNERABILITIES,
            Permission.VIEW_ESCALATIONS,
            Permission.VIEW_TRIAGE,
            Permission.VIEW_REMEDIATIONS
    )),
    ANALYST(Set.of(
            Permission.VIEW_VULNERABILITIES,
            Permission.MANAGE_VULNERABILITIES,
            Permission.VIEW_ESCALATIONS,
            Permission.MANAGE_ESCALATIONS,
            Permission.VIEW_TRIAGE,
            Permission.MANAGE_TRIAGE,
            Permission.VIEW_REMEDIATIONS,
            Permission.MANAGE_REMEDIATIONS
    )),
    LEAD(Set.of(
            Permission.VIEW_VULNERABILITIES,
            Permission.MANAGE_VULNERABILITIES,
            Permission.VIEW_ESCALATIONS,
            Permission.MANAGE_ESCALATIONS,
            Permission.VIEW_TRIAGE,
            Permission.MANAGE_TRIAGE,
            Permission.VIEW_REMEDIATIONS,
            Permission.MANAGE_REMEDIATIONS,
            Permission.VIEW_ADMIN
    )),
    ADMIN(Set.of(Permission.values()));

    private static final Map<String, Role> GROUP_MAPPING = Map.of(
            "ephor-viewers", VIEWER,
            "ephor-analysts", ANALYST,
            "ephor-leads", LEAD,
            "ephor-admins", ADMIN
    );

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public static Set<Permission> resolvePermissions(List<String> userGroups) {
        if (userGroups == null || userGroups.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Permission> resolved = EnumSet.noneOf(Permission.class);
        for (String group : userGroups) {
            Role role = GROUP_MAPPING.get(group);
            if (role != null) {
                resolved.addAll(role.permissions);
            }
        }
        return resolved;
    }
}
