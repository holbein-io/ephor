package io.holbein.ephor.api.dto.escalation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to create a new escalation")
public record CreateEscalationRequest(
        @NotNull @Schema(description = "Vulnerability ID to escalate")
        Long vulnerabilityId,

        @Min(1) @Max(5) @Schema(description = "Escalation level (1-5)")
        int escalationLevel,

        @NotBlank @Schema(description = "Who escalated this vulnerability")
        String escalatedBy,

        @Schema(description = "Reason for escalation")
        String reason
) {}
