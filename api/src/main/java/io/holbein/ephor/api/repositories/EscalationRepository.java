package io.holbein.ephor.api.repositories;

import io.holbein.ephor.api.entity.Escalation;
import io.holbein.ephor.api.model.enums.EscalationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EscalationRepository extends JpaRepository<Escalation, Long> {

    long countByStatusIn(List<EscalationStatus> statuses);

    @Query("SELECT COUNT(DISTINCT e.vulnerability.id) FROM Escalation e")
    long countDistinctVulnerabilities();
}
