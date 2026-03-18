package io.holbein.ephor.api.directory;

import io.holbein.ephor.api.dto.user.UserDirectoryCapabilities;
import io.holbein.ephor.api.entity.KnownUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "ephor.user-directory.provider", havingValue = "none")
public class NoneUserDirectoryProvider implements UserDirectoryProvider {

    @Override
    public String getName() {
        return "none";
    }

    @Override
    public UserDirectoryCapabilities getCapabilities() {
        return new UserDirectoryCapabilities("none", false, false, false, false);
    }

    @Override
    public List<KnownUser> searchUsers(String query, int limit) {
        return List.of();
    }

    @Override
    public boolean isValidAssignee(String username) {
        return true;
    }

    @Override
    public void syncUsers() {
        // no-op
    }
}
