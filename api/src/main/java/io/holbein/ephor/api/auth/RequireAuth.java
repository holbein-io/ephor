package io.holbein.ephor.api.auth;

import io.holbein.ephor.api.model.enums.Permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to require authentication for a controller method or class.
 *
 * Usage:
 *   @RequireAuth
 *   public ResponseEntity<?> protectedEndpoint() { ... }
 *
 *   @RequireAuth(groups = {"admin", "security-team"})
 *   public ResponseEntity<?> adminOnlyEndpoint() { ... }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuth {

    /**
     * Required groups. User must belong to at least one of these groups.
     * Empty array means any authenticated user is allowed.
     */
    String[] groups() default {};

    /**
     * If true, user must belong to ALL specified groups.
     * If false (default), user must belong to at least ONE group.
     */
    boolean requireAllGroups() default false;

    /**
     * Required permissions. User must have at least one of these permissions.
     * Empty array means no permission check is performed.
     */
    Permission[] permissions() default {};
}
