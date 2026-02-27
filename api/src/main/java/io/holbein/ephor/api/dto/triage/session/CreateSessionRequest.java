package io.holbein.ephor.api.dto.triage.session;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Request to create a new triage session in PREPARING status")
public record CreateSessionRequest(

        @Schema(description = "Date of the triage session", example = "2026-02-15")
        @NotNull
        LocalDate sessionDate,

        @Schema(description = "Username of the preparation lead", example = "alice")
        @NotNull
        String prepLead,

        @Schema(description = "Initial preparation notes")
        String prepNotes,

        @Schema(description = "List of attendee usernames")
        List<String> attendees,

        @Schema(description = "General session notes")
        String notes
) {}
