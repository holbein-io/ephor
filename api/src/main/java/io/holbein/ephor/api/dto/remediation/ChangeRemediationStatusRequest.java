package io.holbein.ephor.api.dto.remediation;

import io.holbein.ephor.api.model.enums.CompletionMethod;
import io.holbein.ephor.api.model.enums.RemediationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to change remediation status with state machine validation")
public record ChangeRemediationStatusRequest(

        @Schema(description = "Target status", example = "in_progress")
        @NotNull
        RemediationStatus status,

        @Schema(description = "Required when status = completed", example = "version_upgrade")
        CompletionMethod completionMethod,

        @Schema(description = "Required when status = completed", example = "alice")
        String completedBy,

        @Schema(description = "Required when status = abandoned (reason for abandoning)")
        String notes
) {}
