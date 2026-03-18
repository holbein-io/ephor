package io.holbein.ephor.api.unit;

import io.holbein.ephor.api.model.enums.Permission;
import io.holbein.ephor.api.model.enums.Role;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RolePermissionTest {

    @Test
    void resolvePermissions_nullGroups_returnsEmpty() {
        Set<Permission> result = Role.resolvePermissions(null);
        assertThat(result).isEmpty();
    }

    @Test
    void resolvePermissions_emptyGroups_returnsEmpty() {
        Set<Permission> result = Role.resolvePermissions(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void resolvePermissions_unknownGroup_returnsEmpty() {
        Set<Permission> result = Role.resolvePermissions(List.of("some-random-group"));
        assertThat(result).isEmpty();
    }

    @Test
    void resolvePermissions_viewerGroup_returnsViewOnly() {
        Set<Permission> result = Role.resolvePermissions(List.of("ephor-viewers"));

        assertThat(result).containsExactlyInAnyOrder(
                Permission.VIEW_VULNERABILITIES,
                Permission.VIEW_ESCALATIONS,
                Permission.VIEW_TRIAGE,
                Permission.VIEW_REMEDIATIONS
        );
        assertThat(result).doesNotContain(
                Permission.MANAGE_VULNERABILITIES,
                Permission.MANAGE_ADMIN,
                Permission.VIEW_ADMIN
        );
    }

    @Test
    void resolvePermissions_analystGroup_includesManagePermissions() {
        Set<Permission> result = Role.resolvePermissions(List.of("ephor-analysts"));

        assertThat(result).contains(
                Permission.VIEW_VULNERABILITIES,
                Permission.MANAGE_VULNERABILITIES,
                Permission.VIEW_ESCALATIONS,
                Permission.MANAGE_ESCALATIONS,
                Permission.VIEW_TRIAGE,
                Permission.MANAGE_TRIAGE,
                Permission.VIEW_REMEDIATIONS,
                Permission.MANAGE_REMEDIATIONS
        );
        assertThat(result).doesNotContain(Permission.VIEW_ADMIN, Permission.MANAGE_ADMIN);
    }

    @Test
    void resolvePermissions_leadGroup_includesViewAdmin() {
        Set<Permission> result = Role.resolvePermissions(List.of("ephor-leads"));

        assertThat(result).contains(Permission.VIEW_ADMIN);
        assertThat(result).doesNotContain(Permission.MANAGE_ADMIN);
    }

    @Test
    void resolvePermissions_adminGroup_includesAllPermissions() {
        Set<Permission> result = Role.resolvePermissions(List.of("ephor-admins"));

        assertThat(result).containsExactlyInAnyOrder(Permission.values());
    }

    @Test
    void resolvePermissions_multipleGroups_mergesPermissions() {
        Set<Permission> result = Role.resolvePermissions(List.of("ephor-viewers", "ephor-leads"));

        // Should have viewer + lead permissions combined
        assertThat(result).contains(
                Permission.VIEW_VULNERABILITIES,
                Permission.VIEW_ADMIN,
                Permission.MANAGE_VULNERABILITIES
        );
        assertThat(result).doesNotContain(Permission.MANAGE_ADMIN);
    }

    @Test
    void resolvePermissions_mixedKnownAndUnknownGroups_ignoresUnknown() {
        Set<Permission> result = Role.resolvePermissions(
                List.of("developers", "ephor-viewers", "security-team"));

        assertThat(result).containsExactlyInAnyOrder(
                Permission.VIEW_VULNERABILITIES,
                Permission.VIEW_ESCALATIONS,
                Permission.VIEW_TRIAGE,
                Permission.VIEW_REMEDIATIONS
        );
    }

    @Test
    void viewer_hasNoManagePermissions() {
        Set<Permission> viewerPerms = Role.VIEWER.getPermissions();

        assertThat(viewerPerms).allSatisfy(p ->
                assertThat(p.name()).startsWith("VIEW_"));
    }

    @Test
    void admin_hasSupersetOfAllOtherRoles() {
        Set<Permission> adminPerms = Role.ADMIN.getPermissions();

        assertThat(adminPerms).containsAll(Role.VIEWER.getPermissions());
        assertThat(adminPerms).containsAll(Role.ANALYST.getPermissions());
        assertThat(adminPerms).containsAll(Role.LEAD.getPermissions());
    }
}
