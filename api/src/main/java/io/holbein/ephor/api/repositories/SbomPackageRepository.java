package io.holbein.ephor.api.repositories;

import io.holbein.ephor.api.entity.SbomPackage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SbomPackageRepository extends JpaRepository<SbomPackage, UUID> {

    @Modifying
    @Query("DELETE FROM SbomPackage sp WHERE sp.sbomDocument.id = :sbomId")
    void deleteBySbomDocumentId(@Param("sbomId") UUID sbomId);

    @Query("SELECT sp FROM SbomPackage sp WHERE LOWER(sp.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<SbomPackage> searchByName(@Param("name") String name, Pageable pageable);

    @Query("SELECT sp FROM SbomPackage sp WHERE LOWER(sp.name) LIKE LOWER(CONCAT('%', :name, '%')) AND sp.type = :type")
    Page<SbomPackage> searchByNameAndType(@Param("name") String name, @Param("type") String type, Pageable pageable);

    @Query("SELECT DISTINCT sp.imageReference FROM SbomPackage sp WHERE sp.name = :name")
    List<String> findImagesByPackageName(@Param("name") String name);

    @Query("SELECT DISTINCT sp.imageReference FROM SbomPackage sp WHERE sp.name = :name AND sp.version = :version")
    List<String> findImagesByPackageNameAndVersion(@Param("name") String name, @Param("version") String version);

    @Query("""
        SELECT sp.name, sp.version, sp.type, COUNT(DISTINCT sp.imageReference) as imageCount
        FROM SbomPackage sp
        GROUP BY sp.name, sp.version, sp.type
        ORDER BY imageCount DESC
        """)
    Page<Object[]> findTopPackages(Pageable pageable);

    List<SbomPackage> findBySbomDocumentId(UUID sbomId);

    @Query("""
        SELECT sp.license,
               COUNT(DISTINCT CONCAT(sp.name, '@', sp.version, '@', COALESCE(sp.type, ''))),
               COUNT(DISTINCT sp.imageReference)
        FROM SbomPackage sp
        WHERE sp.license IS NOT NULL
        GROUP BY sp.license
        ORDER BY COUNT(DISTINCT CONCAT(sp.name, '@', sp.version, '@', COALESCE(sp.type, ''))) DESC
        """)
    List<Object[]> getLicenseDistribution();

    @Query("""
        SELECT sp.name, sp.version, sp.type, COUNT(DISTINCT sp.imageReference)
        FROM SbomPackage sp
        WHERE sp.license = :license
        GROUP BY sp.name, sp.version, sp.type
        ORDER BY COUNT(DISTINCT sp.imageReference) DESC
        """)
    Page<Object[]> findByLicense(@Param("license") String license, Pageable pageable);
}
