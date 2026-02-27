package io.holbein.ephor.api.controller;

import io.holbein.ephor.api.auth.UserContext;
import io.holbein.ephor.api.auth.UserContextHolder;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

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

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return UserContextHolder.getContext()
                .map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElse(ResponseEntity.status(401).build());
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
        return ResponseEntity.ok(config);
    }

    @GetMapping("/logout")
    public ResponseEntity<Map<String, String>> getLogoutUrl() {
        return ResponseEntity.ok(Map.of("logoutUrl", logoutUrl));
    }

    public record UserResponse(
            String username,
            String email,
            String displayName,
            java.util.List<String> groups
    ) {
        public static UserResponse from(UserContext context) {
            return new UserResponse(
                    context.username(),
                    context.email(),
                    context.getEffectiveDisplayName(),
                    context.groups()
            );
        }
    }
}
