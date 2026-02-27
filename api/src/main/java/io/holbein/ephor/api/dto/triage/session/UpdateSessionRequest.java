package io.holbein.ephor.api.dto.triage.session;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Request to update triage session metadata")
public record UpdateSessionRequest(

        @Schema(description = "Updated preparation notes")
        String prepNotes,

        @Schema(description = "Updated session notes")
        String notes,

        @Schema(description = "Updated list of attendee usernames")
        List<String> attendees
) {}
