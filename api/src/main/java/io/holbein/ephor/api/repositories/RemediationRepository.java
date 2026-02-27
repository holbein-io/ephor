package io.holbein.ephor.api.repositories;

import io.holbein.ephor.api.entity.Remediation;
import io.holbein.ephor.api.model.enums.RemediationPriority;
import io.holbein.ephor.api.model.enums.RemediationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RemediationRepository extends JpaRepository<Remediation, Long> {

    List<Remediation> findAllByVulnerabilityId(Long vulnerabilityId);

    @Query("SELECT r FROM Remediation r " +
           "WHERE r.status IN :statuses AND r.targetDate < :date " +
           "ORDER BY r.targetDate ASC")
    List<Remediation> findOverdue(@Param("statuses") List<RemediationStatus> statuses,
                                  @Param("date") LocalDate date);

    @Query("SELECT r FROM Remediation r " +
           "WHERE r.status IN :statuses AND r.targetDate < :date " +
           "AND (:priority IS NULL OR r.priority = :priority) " +
           "AND (:assignedTo IS NULL OR r.assignedTo = :assignedTo) " +
           "ORDER BY r.targetDate ASC")
    List<Remediation> findOverdueFiltered(@Param("statuses") List<RemediationStatus> statuses,
                                          @Param("date") LocalDate date,
                                          @Param("priority") RemediationPriority priority,
                                          @Param("assignedTo") String assignedTo);

    long countByStatus(RemediationStatus status);

    @Query("SELECT COUNT(r) FROM Remediation r " +
           "WHERE r.status IN :statuses AND r.targetDate < :date")
    long countOverdue(@Param("statuses") List<RemediationStatus> statuses,
                      @Param("date") LocalDate date);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (completed_at - created_at)) / 86400) " +
                   "FROM remediations WHERE status = 'completed'",
           nativeQuery = true)
    Double avgCompletionDays();
}
