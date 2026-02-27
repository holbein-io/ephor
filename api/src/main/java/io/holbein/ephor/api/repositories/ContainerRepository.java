package io.holbein.ephor.api.repositories;

import io.holbein.ephor.api.entity.Container;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContainerRepository extends JpaRepository<Container, Long> {

    Optional<Container> findByWorkloadIdAndName(Long workloadId, String name);
}
