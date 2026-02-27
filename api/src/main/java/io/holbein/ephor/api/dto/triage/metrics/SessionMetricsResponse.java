package io.holbein.ephor.api.dto.triage.metrics;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Metrics and analytics for a completed triage session")
public record SessionMetricsResponse(
        long sessionId,
        int totalVulnerabilities,
        int decisionsMade,

        @Schema(description = "Breakdown of decisions by status")
        DecisionBreakdown decisionBreakdown,

        int bulkOperationsCount,
        int individualDecisionsCount,
        Integer prepDurationMinutes,
        Integer sessionDurationMinutes,

        @Schema(description = "Percentage of preparations that received a decision")
        Double completionRate,

        @Schema(description = "Average decisions per minute")
        Double efficiencyScore,

        Instant calculatedAt
) {
    @Schema(description = "Count of decisions by status type")
    public record DecisionBreakdown(
            int acceptedRisk,
            int falsePositive,
            int needsRemediation,
            int duplicate
    ) {}
}
