package io.holbein.ephor.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User directory provider capabilities")
public record UserDirectoryCapabilities(
        String provider,
        boolean userSearchEnabled,
        boolean strictAssignment,
        boolean myItemsEnabled,
        boolean userSyncEnabled
) {}
