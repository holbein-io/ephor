package io.holbein.ephor.api.dto.triage.session;

import io.holbein.ephor.api.model.enums.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to transition triage session status")
public record ChangeSessionStatusRequest(

        @Schema(description = "Target status for the session", example = "ACTIVE")
        @NotNull
        SessionStatus status,

        @Schema(description = "Username performing the action", example = "alice")
        @NotNull
        String user,

        @Schema(description = "Reason for cancellation (only for CANCELLED transition)")
        String reason,

        @Schema(description = "Final notes (only for COMPLETED transition)")
        String notes
) {}
