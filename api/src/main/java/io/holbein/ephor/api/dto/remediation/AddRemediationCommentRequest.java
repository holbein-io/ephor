package io.holbein.ephor.api.dto.remediation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to add a comment to a remediation's activity log")
public record AddRemediationCommentRequest(

        @Schema(description = "Author username", example = "alice")
        @NotNull
        String author,

        @Schema(description = "Comment text")
        @NotNull
        String comment
) {}
