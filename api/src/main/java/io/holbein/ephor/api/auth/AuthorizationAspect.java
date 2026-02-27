package io.holbein.ephor.api.auth;

import io.holbein.ephor.api.exception.ForbiddenException;
import io.holbein.ephor.api.exception.UnauthorizedException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Aspect that enforces @RequireAuth annotations on controller methods.
 *
 * Throws:
 * - UnauthorizedException (401) if user is not authenticated
 * - ForbiddenException (403) if user lacks required groups
 */
@Aspect
@Component
public class AuthorizationAspect {

    @Around("@annotation(io.holbein.ephor.api.auth.RequireAuth) || @within(io.holbein.ephor.api.auth.RequireAuth)")
    public Object checkAuthorization(ProceedingJoinPoint joinPoint) throws Throwable {
        RequireAuth annotation = getAnnotation(joinPoint);

        if (annotation == null) {
            return joinPoint.proceed();
        }

        // Check authentication
        UserContext user = UserContextHolder.getContext()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        // Check group authorization if groups are specified
        String[] requiredGroups = annotation.groups();
        if (requiredGroups.length > 0) {
            List<String> groupList = Arrays.asList(requiredGroups);

            boolean authorized;
            if (annotation.requireAllGroups()) {
                authorized = user.hasAllGroups(groupList);
            } else {
                authorized = user.hasAnyGroup(groupList);
            }

            if (!authorized) {
                throw new ForbiddenException(
                        "Access denied. Required groups: " + groupList +
                                ", user groups: " + user.groups()
                );
            }
        }

        return joinPoint.proceed();
    }

    private RequireAuth getAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // Check method-level annotation first
        RequireAuth annotation = method.getAnnotation(RequireAuth.class);
        if (annotation != null) {
            return annotation;
        }

        // Fall back to class-level annotation
        return method.getDeclaringClass().getAnnotation(RequireAuth.class);
    }
}
