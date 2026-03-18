package io.holbein.ephor.api.dto.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to create a comment")
public record CreateCommentRequest(
        @NotBlank(message = "Comment body cannot be blank") String body
) {}
