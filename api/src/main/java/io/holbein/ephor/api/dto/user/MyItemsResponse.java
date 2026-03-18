package io.holbein.ephor.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Items assigned to or created by the current user")
public record MyItemsResponse(
        List<MyRemediation> remediations,
        List<MyEscalation> escalations,
        List<MyComment> recentComments
) {
    public record MyRemediation(Long id, Long vulnerabilityId, String cveId, String status, String targetDate) {}
    public record MyEscalation(Long id, Long vulnerabilityId, String cveId, String status, String escalatedAt) {}
    public record MyComment(Long id, String entityType, Long entityId, Long vulnerabilityId, String cveId, String body, String createdAt) {}
}
