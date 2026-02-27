package io.holbein.ephor.api.repositories;

import io.holbein.ephor.api.entity.TriagePreparation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TriagePreparationsRepository extends JpaRepository<TriagePreparation, Long> {
}
