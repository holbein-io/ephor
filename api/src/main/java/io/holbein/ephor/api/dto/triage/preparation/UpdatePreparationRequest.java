package io.holbein.ephor.api.dto.triage.preparation;

import io.holbein.ephor.api.model.enums.PreliminaryDecision;
import io.holbein.ephor.api.model.enums.PrepStatus;
import io.holbein.ephor.api.model.enums.PriorityFlag;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to update a preparation entry")
public record UpdatePreparationRequest(

        @Schema(description = "Updated preparation notes")
        String prepNotes,

        @Schema(description = "Updated priority flag", example = "critical")
        PriorityFlag priorityFlag,

        @Schema(description = "Updated preparation status", example = "flagged")
        PrepStatus prepStatus,

        @Schema(description = "Updated preliminary decision hint")
        PreliminaryDecision preliminaryDecision
) {}
