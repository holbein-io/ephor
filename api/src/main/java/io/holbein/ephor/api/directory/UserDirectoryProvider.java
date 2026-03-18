package io.holbein.ephor.api.directory;

import io.holbein.ephor.api.dto.user.UserDirectoryCapabilities;
import io.holbein.ephor.api.entity.KnownUser;

import java.util.List;

public interface UserDirectoryProvider {

    String getName();

    UserDirectoryCapabilities getCapabilities();

    List<KnownUser> searchUsers(String query, int limit);

    boolean isValidAssignee(String username);

    void syncUsers();
}
