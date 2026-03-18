package io.holbein.ephor.api.directory;

import io.holbein.ephor.api.dto.user.UserDirectoryCapabilities;
import io.holbein.ephor.api.entity.KnownUser;
import io.holbein.ephor.api.repositories.KnownUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ephor.user-directory.provider", havingValue = "internal", matchIfMissing = true)
public class InternalUserDirectoryProvider implements UserDirectoryProvider {

    private final KnownUserRepository knownUserRepository;

    @Override
    public String getName() {
        return "internal";
    }

    @Override
    public UserDirectoryCapabilities getCapabilities() {
        return new UserDirectoryCapabilities("internal", true, false, true, false);
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
    public void syncUsers() {
        // no-op — users are registered on login
    }
}
