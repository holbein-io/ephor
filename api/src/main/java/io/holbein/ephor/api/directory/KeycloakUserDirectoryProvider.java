package io.holbein.ephor.api.directory;

import io.holbein.ephor.api.dto.user.UserDirectoryCapabilities;
import io.holbein.ephor.api.entity.KnownUser;
import io.holbein.ephor.api.repositories.KnownUserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ephor.user-directory.provider", havingValue = "keycloak")
public class KeycloakUserDirectoryProvider implements UserDirectoryProvider {

    private final KnownUserRepository knownUserRepository;
    private final KeycloakDirectoryProperties properties;
    private final PlatformTransactionManager transactionManager;
    private final ClientHttpRequestFactory outboundRequestFactory;

    private RestClient restClient;
    private TransactionTemplate transactionTemplate;

    @PostConstruct
    void init() {
        // Static builder on purpose: Boot's injected one carries the global SNAKE_CASE naming,
        // which would silently null Keycloak's camelCase firstName/lastName.
        this.restClient = RestClient.builder()
                .requestFactory(outboundRequestFactory)
                .baseUrl(properties.getServerUrl())
                .build();
        this.transactionTemplate = new TransactionTemplate(transactionManager);
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
    @Scheduled(fixedDelayString = "${ephor.user-directory.keycloak.sync-interval:900000}")
    public void syncUsers() {
        log.info("Starting Keycloak user sync for realm '{}'", properties.getRealm());
        try {
            String accessToken = obtainServiceToken();
            List<KeycloakUser> keycloakUsers = fetchRealmUsers(accessToken);

            List<SyncedUser> users = new ArrayList<>();
            for (KeycloakUser kcUser : keycloakUsers) {
                if (!kcUser.enabled()) {
                    continue;
                }
                List<String> groups = fetchUserGroups(accessToken, kcUser.id());
                users.add(new SyncedUser(kcUser.username(), kcUser.email(),
                        buildDisplayName(kcUser), String.join(",", groups)));
            }

            // Written only after every HTTP call is done, so the sync never holds a DB connection while waiting on Keycloak.
            transactionTemplate.executeWithoutResult(tx -> users.forEach(user ->
                    knownUserRepository.upsert(user.username(), user.email(), user.displayName(), user.groupsCsv())));

            log.info("Keycloak user sync complete: {} users synced", users.size());
        } catch (Exception e) {
            log.error("Keycloak user sync failed", e);
        }
    }

    private String obtainServiceToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());

        Map<String, Object> response = restClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", properties.getRealm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        return (String) response.get("access_token");
    }

    private List<KeycloakUser> fetchRealmUsers(String accessToken) {
        return restClient.get()
                .uri("/admin/realms/{realm}/users?max=1000", properties.getRealm())
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<List<KeycloakUser>>() {});
    }

    private List<String> fetchUserGroups(String accessToken, String userId) {
        List<KeycloakGroup> groups = restClient.get()
                .uri("/admin/realms/{realm}/users/{userId}/groups", properties.getRealm(), userId)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<List<KeycloakGroup>>() {});

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

    private record SyncedUser(String username, String email, String displayName, String groupsCsv) {}
}
