package io.holbein.ephor.api.repositories;

import io.holbein.ephor.api.entity.Scan;
import io.holbein.ephor.api.model.enums.ScanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScanRepository extends JpaRepository<Scan, Long> {

    @Query("SELECT s  from Scan s " +
            "ORDER BY s.id " +
            "LIMIT :limit"
    )
    List<Scan> findAllWithLimit(int limit);

    @Modifying
    @Query("UPDATE Scan s SET s.status = :status WHERE s.id = :id")
    int updateStatus(@Param("id") long id, @Param("status") ScanStatus status);
}
