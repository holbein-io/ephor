package io.holbein.ephor.api.directory;

import io.holbein.ephor.api.dto.user.UserDirectoryCapabilities;
import io.holbein.ephor.api.entity.KnownUser;
import io.holbein.ephor.api.repositories.KnownUserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ephor.user-directory.provider", havingValue = "github")
public class GitHubUserDirectoryProvider implements UserDirectoryProvider {

    private final KnownUserRepository knownUserRepository;
    private final GitHubDirectoryProperties properties;
    private final PlatformTransactionManager transactionManager;
    private final ClientHttpRequestFactory outboundRequestFactory;

    private RestClient restClient;
    private TransactionTemplate transactionTemplate;

    @PostConstruct
    void init() {
        this.restClient = RestClient.builder()
                .requestFactory(outboundRequestFactory)
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("Authorization", "Bearer " + properties.getToken())
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        log.info("GitHub user directory provider initialized: org={}", properties.getOrg());
    }

    @Override
    public String getName() {
        return "github";
    }

    @Override
    public UserDirectoryCapabilities getCapabilities() {
        return new UserDirectoryCapabilities("github", true, true, true, true);
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
    @Scheduled(fixedDelayString = "${ephor.user-directory.github.sync-interval:1800000}")
    public void syncUsers() {
        log.info("Starting GitHub user sync for org '{}'", properties.getOrg());
        try {
            Map<String, Set<String>> userGroups = new HashMap<>();

            // Fetch members for each mapped team
            for (Map.Entry<String, String> mapping : properties.getTeamMapping().entrySet()) {
                String teamSlug = mapping.getKey();
                String ephorGroup = mapping.getValue();

                List<GitHubUser> members = fetchTeamMembers(teamSlug);
                for (GitHubUser member : members) {
                    userGroups.computeIfAbsent(member.login(), k -> new LinkedHashSet<>())
                            .add(ephorGroup);
                }

                log.debug("Fetched {} members from team '{}'", members.size(), teamSlug);
            }

            // If no team mapping, fetch all org members
            if (properties.getTeamMapping().isEmpty()) {
                List<GitHubUser> orgMembers = fetchOrgMembers();
                for (GitHubUser member : orgMembers) {
                    userGroups.computeIfAbsent(member.login(), k -> new LinkedHashSet<>());
                }
                log.debug("Fetched {} org members (no team mapping)", orgMembers.size());
            }

            List<SyncedUser> users = new ArrayList<>();
            for (Map.Entry<String, Set<String>> entry : userGroups.entrySet()) {
                String username = entry.getKey();
                String groupsCsv = entry.getValue().stream().sorted().collect(Collectors.joining(","));

                // Fetch user profile for email
                GitHubUserProfile profile = fetchUserProfile(username);
                String email = profile != null ? profile.email() : null;
                String displayName = profile != null && profile.name() != null ? profile.name() : username;

                users.add(new SyncedUser(username, email, displayName, groupsCsv));
            }

            // Written only after every HTTP call is done, so the sync never holds a DB connection while waiting on GitHub.
            transactionTemplate.executeWithoutResult(tx -> users.forEach(user ->
                    knownUserRepository.upsert(user.username(), user.email(), user.displayName(), user.groupsCsv())));

            log.info("GitHub user sync complete: {} users synced", users.size());
        } catch (Exception e) {
            log.error("GitHub user sync failed", e);
        }
    }

    private List<GitHubUser> fetchTeamMembers(String teamSlug) {
        try {
            return restClient.get()
                    .uri("/orgs/{org}/teams/{team}/members?per_page=100", properties.getOrg(), teamSlug)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<GitHubUser>>() {});
        } catch (Exception e) {
            log.warn("Failed to fetch members for team '{}': {}", teamSlug, e.getMessage());
            return List.of();
        }
    }

    private List<GitHubUser> fetchOrgMembers() {
        try {
            return restClient.get()
                    .uri("/orgs/{org}/members?per_page=100", properties.getOrg())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<GitHubUser>>() {});
        } catch (Exception e) {
            log.warn("Failed to fetch org members: {}", e.getMessage());
            return List.of();
        }
    }

    private GitHubUserProfile fetchUserProfile(String username) {
        try {
            return restClient.get()
                    .uri("/users/{username}", username)
                    .retrieve()
                    .body(GitHubUserProfile.class);
        } catch (Exception e) {
            log.debug("Failed to fetch profile for user '{}': {}", username, e.getMessage());
            return null;
        }
    }

    record GitHubUser(String login, Long id) {}

    record GitHubUserProfile(String login, String name, String email) {}

    private record SyncedUser(String username, String email, String displayName, String groupsCsv) {}
}
