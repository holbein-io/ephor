package io.holbein.ephor.api.dto.remediation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "A single comment in the remediation activity log")
public record RemediationCommentResponse(
        long id,
        long remediationId,
        String author,
        String comment,
        Instant createdAt
) {}
