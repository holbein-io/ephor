package io.holbein.ephor.api.dto.remediation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Aggregate remediation statistics")
public record RemediationStatisticsResponse(

        @Schema(description = "Total number of remediations")
        int total,

        @Schema(description = "Count with status = planned")
        int planned,

        @Schema(description = "Count with status = in_progress")
        int inProgress,

        @Schema(description = "Count with status = completed")
        int completed,

        @Schema(description = "Count with status = abandoned")
        int abandoned,

        @Schema(description = "Count past target_date and still planned or in_progress")
        int overdue,

        @Schema(description = "Percentage of remediations completed (completed / total * 100)")
        Double completionRate,

        @Schema(description = "Average days from creation to completion")
        Double avgCompletionDays,

        Instant calculatedAt
) {}
