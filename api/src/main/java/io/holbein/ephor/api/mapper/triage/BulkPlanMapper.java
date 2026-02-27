package io.holbein.ephor.api.mapper.triage;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.holbein.ephor.api.dto.triage.bulkplan.*;
import io.holbein.ephor.api.entity.TriageBulkPlan;
import io.holbein.ephor.api.entity.TriageSession;

import java.util.List;
import java.util.Map;

public final class BulkPlanMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private BulkPlanMapper() {}

    public static TriageBulkPlan toEntity(CreateBulkPlanRequest request, TriageSession session) {
        return TriageBulkPlan.builder()
                .triageSession(session)
                .name(request.name())
                .description(request.description())
                .action(request.action())
                .filters(toFilterMap(request.filters()))
                .metadata(toMetadataMap(request.metadata()))
                .createdBy(request.createdBy())
                .build();
    }

    public static BulkPlanResponse toResponse(TriageBulkPlan entity) {
        return toResponse(entity, List.of());
    }

    public static BulkPlanResponse toResponse(TriageBulkPlan entity,
                                               List<MatchingVulnerability> matchingVulnerabilities) {
        return new BulkPlanResponse(
                entity.getId(),
                entity.getTriageSession().getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getAction(),
                fromFilterMap(entity.getFilters()),
                fromMetadataMap(entity.getMetadata()),
                entity.getStatus(),
                entity.getEstimatedCount(),
                entity.getActualCount(),
                entity.getCreatedDuringPrep(),
                entity.getCreatedBy(),
                entity.getExecutedBy(),
                entity.getExecutedAt(),
                entity.getCreatedAt(),
                matchingVulnerabilities
        );
    }

    public static BulkPlanFilters parseFilters(Map<String, Object> map) {
        return fromFilterMap(map);
    }

    public static BulkPlanMetadata parseMetadata(Map<String, Object> map) {
        return fromMetadataMap(map);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toFilterMap(BulkPlanFilters filters) {
        if (filters == null) return Map.of();
        return OBJECT_MAPPER.convertValue(filters, Map.class);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMetadataMap(BulkPlanMetadata metadata) {
        if (metadata == null) return null;
        return OBJECT_MAPPER.convertValue(metadata, Map.class);
    }

    private static BulkPlanFilters fromFilterMap(Map<String, Object> map) {
        if (map == null) return null;
        return OBJECT_MAPPER.convertValue(map, BulkPlanFilters.class);
    }

    private static BulkPlanMetadata fromMetadataMap(Map<String, Object> map) {
        if (map == null) return null;
        return OBJECT_MAPPER.convertValue(map, BulkPlanMetadata.class);
    }
}
