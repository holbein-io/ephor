package io.holbein.ephor.api.repositories;

import io.holbein.ephor.api.entity.TriageDecision;
import io.holbein.ephor.api.model.enums.DecisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TriageDecisionRepository extends JpaRepository<TriageDecision, Long> {

    // Find decisions by session
    List<TriageDecision> findByTriageSessionIdOrderByCreatedAtDesc(Long sessionId);

    // Find by session and vulnerability (for upsert logic)
    Optional<TriageDecision> findByTriageSessionIdAndVulnerabilityId(Long sessionId, Long vulnerabilityId);

    // Check if decision exists
    boolean existsByTriageSessionIdAndVulnerabilityId(Long sessionId, Long vulnerabilityId);

    // Find decisions by status
    List<TriageDecision> findByDecisionOrderByCreatedAtDesc(DecisionStatus status);

    // Find decisions assigned to an user
    List<TriageDecision> findByAssignedToOrderByTargetDateAsc(String assignedTo);

    // Find decisions with upcoming target dates
    List<TriageDecision> findByTargetDateBeforeAndDecisionOrderByTargetDateAsc(
            LocalDate date, DecisionStatus status
    );

    // Fetch decision with vulnerability details
    @Query("SELECT td FROM TriageDecision td " +
            "LEFT JOIN FETCH td.vulnerability v " +
            "LEFT JOIN FETCH td.remediation r " +
            "WHERE td.triageSession.id = :sessionId " +
            "ORDER BY td.createdAt DESC")
    List<TriageDecision> findBySessionWithDetails(@Param("sessionId") Long sessionId);

    // Decision with vulnerability and remediation projection
    interface DecisionWithDetails {
        Long getId();

        Long getTriageSessionId();

        Long getVulnerabilityId();

        String getStatus();

        String getAssignedTo();

        LocalDate getTargetDate();

        String getNotes();

        // Vulnerability fields
        String getCveId();

        String getPackageName();

        String getPackageVersion();

        String getSeverity();

        String getTitle();

        String getFixedVersion();

        // Remediation fields
        Long getRemediationId();

        String getRemediationStatus();

        String getRemediationPriority();

        LocalDate getRemediationTargetDate();

        String getRemediationAssignedTo();
    }

    // Native query matching the TypeScript getTriageDecisions method
    @Query(value = """
            SELECT
                td.id,
                td.session_id as triage_session_id,
                td.vulnerability_id,
                td.decision as status,
                td.assigned_to,
                td.target_date,
                td.notes,
                td.created_at,
                v.cve_id,
                v.package_name,
                v.package_version,
                v.severity,
                v.title,
                v.fixed_version,
                r.id as remediation_id,
                r.status as remediation_status,
                r.priority as remediation_priority,
                r.target_date as remediation_target_date,
                r.assigned_to as remediation_assigned_to
            FROM triage_decisions td
            JOIN vulnerabilities v ON td.vulnerability_id = v.id
            LEFT JOIN remediations r ON td.id = r.triage_decision_id
            WHERE td.session_id = :sessionId
            ORDER BY td.created_at DESC
            """, nativeQuery = true)
    List<DecisionWithDetails> findDecisionsWithDetailsBySessionId(@Param("sessionId") Long sessionId);

    // Update decision
    @Modifying
    @Query("UPDATE TriageDecision td SET " +
            "td.decision = :decision, " +
            "td.notes = :notes, " +
            "td.assignedTo = :assignedTo, " +
            "td.targetDate = :targetDate " +
            "WHERE td.triageSession.id = :sessionId AND td.vulnerability.id = :vulnerabilityId")
    int updateDecision(@Param("sessionId") Long sessionId,
                       @Param("vulnerabilityId") Long vulnerabilityId,
                       @Param("decision") DecisionStatus decision,
                       @Param("notes") String notes,
                       @Param("assignedTo") String assignedTo,
                       @Param("targetDate") LocalDate targetDate);

    @Query("SELECT td.vulnerability.id FROM TriageDecision td WHERE td.triageSession.id = :sessionId")
    List<Long> findDecidedVulnerabilityIdsBySessionId(@Param("sessionId") Long sessionId);

    // Count by session and decision type
    long countByTriageSessionIdAndDecision(Long sessionId, DecisionStatus decision);

    // Delete all decisions for a session
    @Modifying
    @Query("DELETE FROM TriageDecision td WHERE td.triageSession.id = :sessionId")
    int deleteBySessionId(@Param("sessionId") Long sessionId);

    // Get latest triage info for a vulnerability
    interface TriageInfo {
        Long getDecisionId();
        String getTriageStatus();
        String getTriageNotes();
        String getAssignedTo();
        LocalDate getTargetDate();
        java.time.Instant getDecisionDate();
        LocalDate getSessionDate();
        String[] getAttendees();
        Long getRemediationId();
        String getRemediationStatus();
        String getRemediationPriority();
        java.time.Instant getRemediationCompleted();
    }

    @Query(value = """
            SELECT
                td.id as decision_id,
                td.decision as triage_status,
                td.notes as triage_notes,
                td.assigned_to,
                td.target_date,
                td.created_at as decision_date,
                ts.session_date,
                ts.attendees,
                r.id as remediation_id,
                r.status as remediation_status,
                r.priority as remediation_priority,
                r.completed_at as remediation_completed
            FROM triage_decisions td
            JOIN triage_sessions ts ON td.session_id = ts.id
            LEFT JOIN remediations r ON r.triage_decision_id = td.id
            WHERE td.vulnerability_id = :vulnerabilityId
            ORDER BY td.created_at DESC
            LIMIT 1
            """, nativeQuery = true)
    TriageInfo findLatestTriageInfoByVulnerabilityId(@Param("vulnerabilityId") Long vulnerabilityId);
}
