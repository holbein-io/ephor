package io.holbein.ephor.api.dto.triage.bulkplan;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to execute a bulk plan")
public record ExecuteBulkPlanRequest(

        @Schema(description = "Username of the person executing the plan", example = "alice")
        @NotNull
        String executedBy
) {}
