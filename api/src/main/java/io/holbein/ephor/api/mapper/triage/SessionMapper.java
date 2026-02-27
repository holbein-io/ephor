package io.holbein.ephor.api.mapper.triage;

import io.holbein.ephor.api.dto.triage.session.CreateSessionRequest;
import io.holbein.ephor.api.dto.triage.session.SessionResponse;
import io.holbein.ephor.api.entity.TriageSession;

public final class SessionMapper {

    private SessionMapper() {}

    public static TriageSession toEntity(CreateSessionRequest request) {
        return TriageSession.builder()
                .sessionDate(request.sessionDate())
                .prepLead(request.prepLead())
                .prepNotes(request.prepNotes())
                .attendees(request.attendees())
                .notes(request.notes())
                .build();
    }

    public static SessionResponse toResponse(TriageSession entity) {
        return new SessionResponse(
                entity.getId(),
                entity.getSessionDate(),
                entity.getStatus(),
                entity.getPrepLead(),
                entity.getPrepNotes(),
                entity.getAttendees(),
                entity.getNotes(),
                entity.getPrepCompletedAt(),
                entity.getSessionStartedAt(),
                entity.getCompletedAt(),
                entity.getPrepDurationMinutes(),
                entity.getSessionDurationMinutes(),
                entity.getPreparations().size(),
                entity.getDecisions().size(),
                entity.getBulkPlans().size(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                null // metrics - populated separately when needed
        );
    }
}
