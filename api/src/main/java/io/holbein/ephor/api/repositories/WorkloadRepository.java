package io.holbein.ephor.api.repositories;

import io.holbein.ephor.api.entity.Workload;
import io.holbein.ephor.api.entity.Workload.WorkloadKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkloadRepository extends JpaRepository<Workload, Long> {

    @Query("SELECT DISTINCT w.namespace FROM Workload w ORDER BY w.namespace")
    List<String> findDistinctNamespaces();

    @Query("SELECT w FROM Workload w WHERE w.namespace = :namespace AND w.name = :name AND w.kind = :kind")
    Optional<Workload> findByNaturalKey(@Param("namespace") String namespace,
                                         @Param("name") String name,
                                         @Param("kind") WorkloadKind kind);
}
