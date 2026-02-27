package io.holbein.ephor.api.repositories;

import io.holbein.ephor.api.entity.Scan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScanRepository extends JpaRepository<Scan, Long> {

    @Query("SELECT s  from Scan s " +
            "ORDER BY s.id " +
            "LIMIT :limit"
    )
    List<Scan> findAllWithLimit(int limit);
}
