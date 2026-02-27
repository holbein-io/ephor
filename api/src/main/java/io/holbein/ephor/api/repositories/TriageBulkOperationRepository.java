package io.holbein.ephor.api.repositories;

import io.holbein.ephor.api.entity.TriageBulkOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TriageBulkOperationRepository extends JpaRepository<TriageBulkOperation, Long> {

    List<TriageBulkOperation> findByTriageSessionIdOrderByExecutedAtDesc(Long sessionId);

    List<TriageBulkOperation> findByBulkPlanId(Long bulkPlanId);
}
