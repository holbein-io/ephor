package io.holbein.ephor.api.mapper.remediation;

import io.holbein.ephor.api.dto.remediation.AddRemediationCommentRequest;
import io.holbein.ephor.api.dto.remediation.RemediationCommentResponse;
import io.holbein.ephor.api.entity.Remediation;
import io.holbein.ephor.api.entity.RemediationComment;

public final class RemediationCommentMapper {

    private RemediationCommentMapper() {}

    public static RemediationComment toEntity(AddRemediationCommentRequest request, Remediation remediation) {
        return RemediationComment.builder()
                .remediation(remediation)
                .author(request.author())
                .comment(request.comment())
                .build();
    }

    public static RemediationCommentResponse toResponse(RemediationComment entity) {
        return new RemediationCommentResponse(
                entity.getId(),
                entity.getRemediation().getId(),
                entity.getAuthor(),
                entity.getComment(),
                entity.getCreatedAt()
        );
    }
}
