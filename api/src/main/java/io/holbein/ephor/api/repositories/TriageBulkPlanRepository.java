package io.holbein.ephor.api.repositories;

import io.holbein.ephor.api.entity.TriageBulkPlan;
import io.holbein.ephor.api.model.enums.BulkPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TriageBulkPlanRepository extends JpaRepository<TriageBulkPlan, Long> {
    @Query("SELECT b FROM TriageBulkPlan b " +
            "WHERE b.triageSession.id = :sessionId " +
            "AND b.status = :status")
    List<TriageBulkPlan> findBySessionId(@Param("sessionId") Long sessionId, @Param("status") BulkPlanStatus status);

    List<TriageBulkPlan> findByTriageSessionId(Long sessionId);
}
