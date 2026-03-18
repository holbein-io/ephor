package io.holbein.ephor.api.controller;

import io.holbein.ephor.api.auth.UserContext;
import io.holbein.ephor.api.auth.UserContextHolder;
import io.holbein.ephor.api.model.enums.Permission;
import io.holbein.ephor.api.model.enums.Role;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Authentication status and config")
public class AuthController {

    @Value("${auth.enabled:true}")
    private boolean authEnabled;

    @Value("${auth.login-url:/oauth2/start}")
    private String loginUrl;

    @Value("${auth.logout-url:/oauth2/sign_out}")
    private String logoutUrl;

    @Value("${auth.provider:oauth2-proxy}")
    private String provider;

    @Value("${auth.idp-logout-url:}")
    private String idpLogoutUrl;

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser() {
        return UserContextHolder.getContext()
                .map(user -> ResponseEntity.ok(new AuthResponse(true, UserResponse.from(user), null)))
                .orElse(ResponseEntity.ok(new AuthResponse(false, null, "Not authenticated")));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("authenticated", UserContextHolder.isAuthenticated());

        UserContextHolder.getContext().ifPresent(user -> {
            status.put("username", user.username());
        });

        return ResponseEntity.ok(status);
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("authEnabled", authEnabled);
        config.put("loginUrl", loginUrl);
        config.put("logoutUrl", logoutUrl);
        config.put("provider", provider);
        if (idpLogoutUrl != null && !idpLogoutUrl.isBlank()) {
            config.put("idpLogoutUrl", idpLogoutUrl);
        }
        return ResponseEntity.ok(config);
    }

    @GetMapping("/logout")
    public ResponseEntity<Map<String, String>> getLogoutUrl() {
        return ResponseEntity.ok(Map.of("logoutUrl", logoutUrl));
    }

    public record AuthResponse(
            boolean authenticated,
            UserResponse user,
            String error
    ) {}

    public record UserResponse(
            String username,
            String email,
            String displayName,
            java.util.List<String> groups,
            java.util.List<String> permissions
    ) {
        public static UserResponse from(UserContext context) {
            Set<Permission> perms = Role.resolvePermissions(context.groups());
            java.util.List<String> permissionNames = perms.stream()
                    .map(Enum::name)
                    .sorted()
                    .toList();
            return new UserResponse(
                    context.username(),
                    context.email(),
                    context.getEffectiveDisplayName(),
                    context.groups(),
                    permissionNames
            );
        }
    }
}
