package io.holbein.ephor.api.directory;

import io.holbein.ephor.api.dto.user.UserDirectoryCapabilities;
import io.holbein.ephor.api.entity.KnownUser;
import io.holbein.ephor.api.repositories.KnownUserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ephor.user-directory.provider", havingValue = "keycloak")
public class KeycloakUserDirectoryProvider implements UserDirectoryProvider {

    private final KnownUserRepository knownUserRepository;
    private final KeycloakDirectoryProperties properties;

    private WebClient webClient;

    @PostConstruct
    void init() {
        this.webClient = WebClient.builder()
                .baseUrl(properties.getServerUrl())
                .build();
        log.info("Keycloak user directory provider initialized: server={}, realm={}",
                properties.getServerUrl(), properties.getRealm());
    }

    @Override
    public String getName() {
        return "keycloak";
    }

    @Override
    public UserDirectoryCapabilities getCapabilities() {
        return new UserDirectoryCapabilities("keycloak", true, true, true, true);
    }

    @Override
    public List<KnownUser> searchUsers(String query, int limit) {
        return knownUserRepository.search(query, limit);
    }

    @Override
    public boolean isValidAssignee(String username) {
        return knownUserRepository.existsById(username);
    }

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${ephor.user-directory.keycloak.sync-interval:900000}")
    public void syncUsers() {
        log.info("Starting Keycloak user sync for realm '{}'", properties.getRealm());
        try {
            String accessToken = obtainServiceToken();
            List<KeycloakUser> keycloakUsers = fetchRealmUsers(accessToken);

            int synced = 0;
            for (KeycloakUser kcUser : keycloakUsers) {
                if (!kcUser.enabled()) {
                    continue;
                }
                String username = kcUser.username();
                String email = kcUser.email();
                String displayName = buildDisplayName(kcUser);

                List<String> groups = fetchUserGroups(accessToken, kcUser.id());
                String groupsCsv = String.join(",", groups);

                knownUserRepository.upsert(username, email, displayName, groupsCsv);
                synced++;
            }

            log.info("Keycloak user sync complete: {} users synced", synced);
        } catch (Exception e) {
            log.error("Keycloak user sync failed", e);
        }
    }

    private String obtainServiceToken() {
        Map<String, Object> response = webClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", properties.getRealm())
                .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                        .with("client_id", properties.getClientId())
                        .with("client_secret", properties.getClientSecret()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        return (String) response.get("access_token");
    }

    private List<KeycloakUser> fetchRealmUsers(String accessToken) {
        return webClient.get()
                .uri("/admin/realms/{realm}/users?max=1000", properties.getRealm())
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<KeycloakUser>>() {})
                .block();
    }

    private List<String> fetchUserGroups(String accessToken, String userId) {
        List<KeycloakGroup> groups = webClient.get()
                .uri("/admin/realms/{realm}/users/{userId}/groups", properties.getRealm(), userId)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<KeycloakGroup>>() {})
                .block();

        if (groups == null) {
            return List.of();
        }
        return groups.stream()
                .map(KeycloakGroup::name)
                .toList();
    }

    private String buildDisplayName(KeycloakUser user) {
        if (user.firstName() != null && user.lastName() != null) {
            return user.firstName() + " " + user.lastName();
        }
        if (user.firstName() != null) {
            return user.firstName();
        }
        return user.username();
    }

    record KeycloakUser(String id, String username, String email,
                        String firstName, String lastName, boolean enabled) {}

    record KeycloakGroup(String id, String name, String path) {}
}
