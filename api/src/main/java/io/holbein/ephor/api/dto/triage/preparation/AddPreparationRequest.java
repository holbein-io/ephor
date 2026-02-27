package io.holbein.ephor.api.dto.triage.preparation;

import io.holbein.ephor.api.model.enums.PreliminaryDecision;
import io.holbein.ephor.api.model.enums.PriorityFlag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to add a vulnerability to a triage session during preparation")
public record AddPreparationRequest(

        @Schema(description = "ID of the triage session", example = "1")
        @NotNull
        Long sessionId,

        @Schema(description = "ID of the vulnerability to add", example = "42")
        @NotNull
        Long vulnerabilityId,

        @Schema(description = "Preparation notes to be shown during the active session")
        String prepNotes,

        @Schema(description = "Priority flag for ordering during triage", example = "high")
        PriorityFlag priorityFlag,

        @Schema(description = "Optional preliminary decision hint")
        PreliminaryDecision preliminaryDecision,

        @Schema(description = "Username of the person preparing", example = "alice")
        @NotNull
        String prepBy
) {}
