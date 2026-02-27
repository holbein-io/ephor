package io.holbein.ephor.api.repositories;

import io.holbein.ephor.api.entity.TriageSession;
import io.holbein.ephor.api.model.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TriageSessionRepository extends JpaRepository<TriageSession, Long> {

    // Spring Data derived query - equivalent to getTriageSessions() ordered by date
    List<TriageSession> findAllByOrderBySessionDateDesc();

    // Find by status
    List<TriageSession> findByStatusOrderBySessionDateDesc(SessionStatus status);

    // Find by date range
    List<TriageSession> findBySessionDateBetweenOrderBySessionDateDesc(LocalDate start, LocalDate end);

    // Find sessions with a specific prep lead
    List<TriageSession> findByPrepLeadOrderBySessionDateDesc(String prepLead);

    // Custom JPQL query - fetch session with decisions eagerly
    @Query("SELECT DISTINCT ts FROM TriageSession ts " +
            "LEFT JOIN FETCH ts.decisions d " +
            "LEFT JOIN FETCH d.vulnerability " +
            "WHERE ts.id = :id")
    Optional<TriageSession> findByIdWithDecisions(@Param("id") Long id);

    // Fetch session with all related data
    @Query("SELECT DISTINCT ts FROM TriageSession ts " +
            "LEFT JOIN FETCH ts.decisions " +
            "LEFT JOIN FETCH ts.preparations " +
            "LEFT JOIN FETCH ts.bulkPlans " +
            "WHERE ts.id = :id")
    Optional<TriageSession> findByIdWithAllRelations(@Param("id") Long id);

    // Update session status
    @Modifying
    @Query("UPDATE TriageSession ts SET ts.status = :status WHERE ts.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") SessionStatus status);

    // Update session status with timestamp for ACTIVE
    @Modifying
    @Query("UPDATE TriageSession ts SET ts.status = :status, ts.sessionStartedAt = :startedAt WHERE ts.id = :id")
    int updateStatusWithStartTime(@Param("id") Long id,
                                  @Param("status") SessionStatus status,
                                  @Param("startedAt") Instant startedAt);

    // Update session status with timestamp for COMPLETED
    @Modifying
    @Query("UPDATE TriageSession ts SET ts.status = :status, ts.completedAt = :completedAt WHERE ts.id = :id")
    int updateStatusWithCompletedTime(@Param("id") Long id,
                                      @Param("status") SessionStatus status,
                                      @Param("completedAt") Instant completedAt);

    // Complete session with duration metrics
    @Modifying
    @Query("UPDATE TriageSession ts SET " +
            "ts.status = 'COMPLETED', " +
            "ts.completedAt = :completedAt, " +
            "ts.sessionDurationMinutes = :sessionDuration, " +
            "ts.prepDurationMinutes = :prepDuration " +
            "WHERE ts.id = :id")
    int completeSession(@Param("id") Long id,
                        @Param("completedAt") Instant completedAt,
                        @Param("sessionDuration") Integer sessionDuration,
                        @Param("prepDuration") Integer prepDuration);

    // Count decisions for a session (using native query for complex aggregation)
    @Query(value = "SELECT COUNT(*) FROM triage_decisions WHERE session_id = :sessionId", nativeQuery = true)
    long countDecisionsBySessionId(@Param("sessionId") Long sessionId);

    Optional<TriageSession> getTriageSessionsById(Long id);

    // Session metrics projection interface
    interface SessionMetrics {
        Long getId();

        String getStatus();

        Long getDecisionsMade();

        Long getPreparationsCount();

        Long getNeedsRemediation();

        Long getAcceptedRisk();

        Long getFalsePositive();

        Long getBulkPlansCount();

        Long getBulkPlansExecuted();
    }

    // Get session metrics using native query
    @Query(value = """
            SELECT
                ts.id,
                ts.status,
                COUNT(DISTINCT td.id) as decisions_made,
                COUNT(DISTINCT tp.id) as preparations_count,
                COUNT(DISTINCT CASE WHEN td.decision = 'needs_remediation' THEN td.id END) as needs_remediation,
                COUNT(DISTINCT CASE WHEN td.decision = 'accepted_risk' THEN td.id END) as accepted_risk,
                COUNT(DISTINCT CASE WHEN td.decision = 'false_positive' THEN td.id END) as false_positive,
                COUNT(DISTINCT tbp.id) as bulk_plans_count,
                COUNT(DISTINCT CASE WHEN tbp.executed = true THEN tbp.id END) as bulk_plans_executed
            FROM triage_sessions ts
            LEFT JOIN triage_decisions td ON ts.id = td.session_id
            LEFT JOIN triage_preparations tp ON ts.id = tp.session_id
            LEFT JOIN triage_bulk_plans tbp ON ts.id = tbp.session_id
            WHERE ts.id = :sessionId
            GROUP BY ts.id
            """, nativeQuery = true)
    Optional<SessionMetrics> getSessionMetrics(@Param("sessionId") Long sessionId);

    // Check if session exists and is in specific status
    boolean existsByIdAndStatus(Long id, SessionStatus status);

    // Find active sessions (not completed)
    @Query("SELECT ts FROM TriageSession ts WHERE ts.status IN ('PREPARING', 'ACTIVE') ORDER BY ts.sessionDate DESC")
    List<TriageSession> findActiveSessions();
}
