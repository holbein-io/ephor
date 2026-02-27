package io.holbein.ephor.api.dto.triage.shared;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generic response for delete operations")
public record DeleteResponse(
        boolean success,
        String message
) {}
