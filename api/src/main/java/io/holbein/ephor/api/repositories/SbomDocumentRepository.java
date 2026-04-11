package io.holbein.ephor.api.repositories;

import io.holbein.ephor.api.entity.SbomDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SbomDocumentRepository extends JpaRepository<SbomDocument, UUID> {

    Optional<SbomDocument> findByImageReferenceAndContentHash(String imageReference, String contentHash);

    @Query("SELECT s FROM SbomDocument s WHERE s.imageReference = :imageReference ORDER BY s.lastSeen DESC LIMIT 1")
    Optional<SbomDocument> findLatestByImageReference(@Param("imageReference") String imageReference);

    List<SbomDocument> findAllByImageReferenceOrderByLastSeenDesc(String imageReference);

    @Query("SELECT DISTINCT s.imageReference FROM SbomDocument s WHERE s.imageReference IN :refs")
    Set<String> findExistingImageReferences(@Param("refs") Collection<String> refs);

    @Query("SELECT DISTINCT s.imageReference FROM SbomDocument s ORDER BY s.imageReference")
    List<String> findAllDistinctImageReferences();

    @Query("SELECT COUNT(DISTINCT s.imageReference) FROM SbomDocument s")
    long countDistinctImageReferences();

    @Query("SELECT s.format, COUNT(DISTINCT s.imageReference) FROM SbomDocument s GROUP BY s.format")
    List<Object[]> countByFormat();
}
