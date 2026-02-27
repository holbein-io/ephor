package io.holbein.ephor.api.dto.escalation;

import io.holbein.ephor.api.model.enums.EscalationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Request to update an escalation")
public record UpdateEscalationRequest(
        @Min(1) @Max(5) @Schema(description = "Escalation level (1-5)")
        Integer escalationLevel,

        @Schema(description = "New status")
        EscalationStatus status,

        @Schema(description = "Updated reason")
        String reason,

        @Schema(description = "MS Teams message ID")
        String msTeamsMessageId
) {}
