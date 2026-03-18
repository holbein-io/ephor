package io.holbein.ephor.api.dto.comment;

import io.holbein.ephor.api.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Comment details")
public record CommentResponse(
        Long id,
        String entityType,
        Long entityId,
        String body,
        String commentType,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt
) {
    public static CommentResponse from(Comment entity) {
        return new CommentResponse(
                entity.getId(),
                entity.getEntityType() != null ? entity.getEntityType().name() : null,
                entity.getEntityId(),
                entity.getBody(),
                entity.getCommentType() != null ? entity.getCommentType().name() : null,
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt()
        );
    }
}
