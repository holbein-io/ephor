package io.holbein.ephor.api.dto.triage.bulkplan;

import io.holbein.ephor.api.model.enums.BulkAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to create a session-scoped bulk plan")
public record CreateBulkPlanRequest(

        @Schema(description = "ID of the triage session", example = "1")
        @NotNull
        Long sessionId,

        @Schema(description = "Name of the bulk plan", example = "Accept low-risk dev vulns")
        @NotNull
        String name,

        @Schema(description = "Description of the bulk plan")
        String description,

        @Schema(description = "Action to apply to matching vulnerabilities", example = "accept_risk")
        @NotNull
        BulkAction action,

        @Schema(description = "Filter criteria resolved against session preparations")
        @NotNull
        BulkPlanFilters filters,

        @Schema(description = "Additional metadata, primarily for needs_remediation action")
        BulkPlanMetadata metadata,

        @Schema(description = "Username of the person creating the plan", example = "alice")
        @NotNull
        String createdBy
) {}
