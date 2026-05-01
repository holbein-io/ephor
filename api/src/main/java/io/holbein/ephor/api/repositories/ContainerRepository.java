package io.holbein.ephor.api.repositories;

import io.holbein.ephor.api.entity.Container;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ContainerRepository extends JpaRepository<Container, Long> {

    Optional<Container> findByWorkloadIdAndName(Long workloadId, String name);

    @Query("SELECT COUNT(DISTINCT CONCAT(c.imageName, ':', c.imageTag)) FROM Container c WHERE c.imageName IS NOT NULL")
    long countDistinctImages();
}
