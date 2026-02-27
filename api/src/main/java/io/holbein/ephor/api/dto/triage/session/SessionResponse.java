package io.holbein.ephor.api.dto.triage.session;

import io.holbein.ephor.api.dto.triage.metrics.SessionMetricsResponse;
import io.holbein.ephor.api.model.enums.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Triage session details")
public record SessionResponse(
        long id,
        LocalDate sessionDate,
        SessionStatus status,
        String prepLead,
        String prepNotes,
        List<String> attendees,
        String notes,
        Instant prepCompletedAt,
        Instant sessionStartedAt,
        Instant completedAt,
        Integer prepDurationMinutes,
        Integer sessionDurationMinutes,

        @Schema(description = "Count of preparations in this session")
        int preparationsCount,

        @Schema(description = "Count of decisions made in this session")
        int decisionsCount,

        @Schema(description = "Count of bulk plans in this session")
        int bulkPlansCount,

        Instant createdAt,
        Instant updatedAt,

        @Schema(description = "Session metrics, included when session status is COMPLETED")
        SessionMetricsResponse metrics
) {}
