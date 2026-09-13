package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.sbom.*;
import io.holbein.ephor.api.entity.SbomDocument;
import io.holbein.ephor.api.entity.SbomPackage;
import io.holbein.ephor.api.exception.ProblemType;
import io.holbein.ephor.api.exception.ResourceNotFoundException;
import io.holbein.ephor.api.repositories.ContainerRepository;
import io.holbein.ephor.api.repositories.SbomDocumentRepository;
import io.holbein.ephor.api.repositories.SbomPackageRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SbomQueryService {

    private final SbomDocumentRepository sbomDocumentRepository;
    private final SbomPackageRepository sbomPackageRepository;
    private final ContainerRepository containerRepository;
    private final EntityManager entityManager;

    // --- Document queries ---

    public Optional<SbomDocument> findLatest(String imageReference) {
        return sbomDocumentRepository.findLatestByImageReference(imageReference);
    }

    public List<String> listImageReferences() {
        return sbomDocumentRepository.findAllDistinctImageReferences();
    }

    public List<SbomHistoryEntry> getHistory(String imageReference) {
        return sbomDocumentRepository.findAllByImageReferenceOrderByLastSeenDesc(imageReference)
                .stream()
                .map(this::toHistoryEntry)
                .toList();
    }

    public Optional<SbomMetadata> getMetadata(String imageReference) {
        return sbomDocumentRepository.findLatestByImageReference(imageReference)
                .map(this::toMetadata);
    }

    public SbomCoverageResponse getCoverage() {
        // Coverage = share of deployed images with an SBOM; stale SBOMs for
        // undeployed images are ignored so the ratio reflects the live fleet.
        long deployedImages = containerRepository.countDistinctImages();
        long deployedWithSbom = sbomDocumentRepository.countDeployedImagesWithSbom();

        Map<String, Long> formatBreakdown = new LinkedHashMap<>();
        for (Object[] row : sbomDocumentRepository.countByFormat()) {
            formatBreakdown.put((String) row[0], (Long) row[1]);
        }

        return SbomCoverageResponse.builder()
                .totalImages(deployedImages)
                .imagesWithSbom(deployedWithSbom)
                .formatBreakdown(formatBreakdown)
                .build();
    }

    public Set<String> findExistingImageReferences(Collection<String> imageReferences) {
        if (imageReferences == null || imageReferences.isEmpty()) {
            return Set.of();
        }
        return sbomDocumentRepository.findExistingImageReferences(imageReferences);
    }

    // --- Package queries ---

    public Page<PackageSearchResult> searchPackages(String name, String type, Pageable pageable) {
        Page<SbomPackage> packages = type != null
                ? sbomPackageRepository.searchByNameAndType(name, type, pageable)
                : sbomPackageRepository.searchByName(name, pageable);
        return packages.map(this::toSearchResult);
    }

    public List<String> findImagesByPackage(String name, String version) {
        return version != null
                ? sbomPackageRepository.findImagesByPackageNameAndVersion(name, version)
                : sbomPackageRepository.findImagesByPackageName(name);
    }

    public Page<SbomPackageResponses.TopPackage> getTopPackages(Pageable pageable) {
        return sbomPackageRepository.findTopPackages(pageable).map(this::toTopPackage);
    }

    public List<SbomPackageResponses.LicenseDistribution> getLicenseDistribution() {
        return sbomPackageRepository.getLicenseDistribution().stream()
                .map(row -> SbomPackageResponses.LicenseDistribution.builder()
                        .license((String) row[0])
                        .packageCount((Long) row[1])
                        .imageCount((Long) row[2])
                        .build())
                .toList();
    }

    // Distinct packages with image spread, matching the license distribution counts.
    public Page<SbomPackageResponses.TopPackage> findByLicense(String license, Pageable pageable) {
        return sbomPackageRepository.findByLicense(license, pageable).map(this::toTopPackage);
    }

    // --- Diff ---

    public SbomDiffResult diff(UUID sbomIdA, UUID sbomIdB) {
        sbomDocumentRepository.findById(sbomIdA)
                .orElseThrow(() -> new ResourceNotFoundException(ProblemType.RESOURCE_NOT_FOUND,
                        "SBOM document not found: " + sbomIdA));
        SbomDocument docB = sbomDocumentRepository.findById(sbomIdB)
                .orElseThrow(() -> new ResourceNotFoundException(ProblemType.RESOURCE_NOT_FOUND,
                        "SBOM document not found: " + sbomIdB));

        Map<String, SbomPackage> packagesA = indexByIdentity(sbomPackageRepository.findBySbomDocumentId(sbomIdA));
        Map<String, SbomPackage> packagesB = indexByIdentity(sbomPackageRepository.findBySbomDocumentId(sbomIdB));

        List<SbomDiffResult.PackageEntry> added = new ArrayList<>();
        List<SbomDiffResult.PackageEntry> removed = new ArrayList<>();
        List<SbomDiffResult.VersionChange> changed = new ArrayList<>();
        int unchanged = 0;

        for (Map.Entry<String, SbomPackage> entry : packagesB.entrySet()) {
            SbomPackage pkgB = entry.getValue();
            SbomPackage pkgA = packagesA.get(entry.getKey());

            if (pkgA == null) {
                added.add(toPackageEntry(pkgB));
            } else if (!pkgA.getVersion().equals(pkgB.getVersion())) {
                changed.add(SbomDiffResult.VersionChange.builder()
                        .name(pkgB.getName())
                        .type(pkgB.getType())
                        .oldVersion(pkgA.getVersion())
                        .newVersion(pkgB.getVersion())
                        .build());
            } else {
                unchanged++;
            }
        }

        for (Map.Entry<String, SbomPackage> entry : packagesA.entrySet()) {
            if (!packagesB.containsKey(entry.getKey())) {
                removed.add(toPackageEntry(entry.getValue()));
            }
        }

        return SbomDiffResult.builder()
                .imageReference(docB.getImageReference())
                .added(added)
                .removed(removed)
                .changed(changed)
                .unchangedCount(unchanged)
                .build();
    }

    // --- Unreported CVEs (ADR-007) ---

    // Only the newest document per image, and only images a container currently runs;
    // the same rule getCoverage applies.
    private static final String LATEST_SBOM_CTE = """
        latest_sbom AS (
            SELECT DISTINCT ON (image_reference) id, image_reference
            FROM sbom_documents
            ORDER BY image_reference, last_seen DESC
        )""";

    private static final String LIVE_CTE = """
        live AS (
            SELECT c.id AS container_id, w.namespace, w.name AS workload,
                   concat(c.image_name, ':', c.image_tag) AS image_reference
            FROM containers c
            JOIN workloads w ON w.id = c.workload_id
            WHERE c.image_name IS NOT NULL
        )""";

    // Exclusion is per CVE rather than per vulnerability row, so a second scanner type
    // cannot re-flag what the first already reported on the image.
    private static final String HITS_CTE = """
        hits AS (
            SELECT v.id AS vulnerability_id, v.cve_id, v.severity, v.title, v.kev_listed,
                   v.epss_score, v.published_date, v.fixed_version,
                   sp.image_reference, sp.name AS package_name, sp.version AS package_version,
                   live.namespace, live.workload
            FROM vulnerabilities v
            JOIN sbom_packages sp ON sp.name = v.package_name AND sp.version = v.package_version
            JOIN latest_sbom l ON l.id = sp.sbom_id
            JOIN live ON live.image_reference = sp.image_reference
            WHERE (v.severity IN ('CRITICAL', 'HIGH') OR v.kev_listed)
              AND NOT EXISTS (
                  SELECT 1
                  FROM vulnerability_instances vi
                  JOIN vulnerabilities v2 ON v2.id = vi.vulnerability_id
                  JOIN live l2 ON l2.container_id = vi.container_id
                  WHERE v2.cve_id = v.cve_id
                    AND l2.image_reference = sp.image_reference)
              %s
        )""";

    // vulnerabilities is unique per package row; fold them so one CVE on one image is one row.
    private static final String GROUPED_CTE = """
        grouped AS (
            SELECT cve_id, image_reference,
                   min(vulnerability_id) AS vulnerability_id,
                   min(CASE severity WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' THEN 1
                                     WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 ELSE 4 END) AS severity_rank,
                   bool_or(kev_listed) AS kev_listed,
                   max(epss_score) AS epss_score,
                   min(published_date) AS published_date,
                   min(title) AS title,
                   string_agg(DISTINCT fixed_version, '|') AS fixed_versions,
                   string_agg(DISTINCT package_name || '@' || package_version, '|') AS packages,
                   string_agg(DISTINCT namespace, '|') AS namespaces,
                   string_agg(DISTINCT namespace || '/' || workload, '|') AS workloads
            FROM hits
            GROUP BY cve_id, image_reference
        )""";

    private static final String UNREPORTED_SELECT = """
        SELECT cve_id, image_reference, vulnerability_id,
               CASE severity_rank WHEN 0 THEN 'CRITICAL' WHEN 1 THEN 'HIGH'
                                  WHEN 2 THEN 'MEDIUM' WHEN 3 THEN 'LOW' ELSE 'UNKNOWN' END AS severity,
               kev_listed, epss_score, published_date, title,
               fixed_versions, packages, namespaces, workloads
        FROM grouped
        ORDER BY kev_listed DESC, epss_score DESC NULLS LAST, severity_rank,
                 published_date DESC NULLS LAST, cve_id, image_reference
        """;

    public Page<UnreportedCve> findUnreportedCves(UnreportedCveFilter filter, Pageable pageable) {
        Query query = unreportedQuery(UNREPORTED_SELECT, filter);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<UnreportedCve> content = rows.stream().map(this::toUnreportedCve).toList();

        long total = ((Number) unreportedQuery("SELECT count(*) FROM grouped", filter).getSingleResult()).longValue();
        return new PageImpl<>(content, pageable, total);
    }

    public UnreportedCveSummary summarizeUnreportedCves() {
        Object[] row = (Object[]) unreportedQuery("""
                SELECT count(*),
                       count(*) FILTER (WHERE kev_listed),
                       count(*) FILTER (WHERE epss_score >= 0.1),
                       count(DISTINCT image_reference)
                FROM grouped
                """, UnreportedCveFilter.builder().build()).getSingleResult();
        return UnreportedCveSummary.builder()
                .total(((Number) row[0]).longValue())
                .kevListed(((Number) row[1]).longValue())
                .epssHigh(((Number) row[2]).longValue())
                .images(((Number) row[3]).longValue())
                .build();
    }

    private Query unreportedQuery(String select, UnreportedCveFilter filter) {
        StringBuilder conditions = new StringBuilder();
        if (filter.getNamespace() != null) conditions.append(" AND live.namespace = :namespace");
        if (Boolean.TRUE.equals(filter.getKevOnly())) conditions.append(" AND v.kev_listed");
        if (filter.getMinEpss() != null) conditions.append(" AND v.epss_score >= :minEpss");
        if (filter.getSeverity() != null) conditions.append(" AND v.severity = :severity");

        String sql = "WITH " + LATEST_SBOM_CTE + ",\n" + LIVE_CTE + ",\n"
                + HITS_CTE.formatted(conditions) + ",\n" + GROUPED_CTE + "\n" + select;
        Query query = entityManager.createNativeQuery(sql);
        if (filter.getNamespace() != null) query.setParameter("namespace", filter.getNamespace());
        if (filter.getMinEpss() != null) query.setParameter("minEpss", filter.getMinEpss());
        if (filter.getSeverity() != null) query.setParameter("severity", filter.getSeverity().toUpperCase());
        return query;
    }

    // --- Mappers ---

    private SbomMetadata toMetadata(SbomDocument doc) {
        return SbomMetadata.builder()
                .id(doc.getId())
                .imageReference(doc.getImageReference())
                .format(doc.getFormat())
                .firstSeen(doc.getFirstSeen())
                .lastSeen(doc.getLastSeen())
                .packageCount(extractPackageCount(doc))
                .build();
    }

    private int extractPackageCount(SbomDocument doc) {
        Map<String, Object> document = doc.getDocument();
        if (document == null) {
            return 0;
        }
        return switch (doc.getFormat()) {
            case "cyclonedx" -> {
                Object components = document.get("components");
                yield components instanceof List<?> list ? list.size() : 0;
            }
            case "spdx" -> {
                Object packages = document.get("packages");
                yield packages instanceof List<?> list ? list.size() : 0;
            }
            default -> 0;
        };
    }

    private SbomHistoryEntry toHistoryEntry(SbomDocument doc) {
        return SbomHistoryEntry.builder()
                .id(doc.getId())
                .imageReference(doc.getImageReference())
                .imageDigest(doc.getImageDigest())
                .contentHash(doc.getContentHash())
                .format(doc.getFormat())
                .scanGroupId(doc.getScanGroupId())
                .firstSeen(doc.getFirstSeen())
                .lastSeen(doc.getLastSeen())
                .build();
    }

    private SbomPackageResponses.TopPackage toTopPackage(Object[] row) {
        return SbomPackageResponses.TopPackage.builder()
                .name((String) row[0])
                .version((String) row[1])
                .type((String) row[2])
                .imageCount((Long) row[3])
                .build();
    }

    private UnreportedCve toUnreportedCve(Object[] row) {
        return UnreportedCve.builder()
                .cveId((String) row[0])
                .imageReference((String) row[1])
                .vulnerabilityId(((Number) row[2]).longValue())
                .severity((String) row[3])
                .kevListed(Boolean.TRUE.equals(row[4]))
                .epssScore(row[5] == null ? null : ((Number) row[5]).doubleValue())
                .publishedDate(row[6] != null ? toInstant(row[6]) : null)
                .title((String) row[7])
                .fixedVersions(splitAgg((String) row[8]))
                .packages(splitAgg((String) row[9]))
                .namespaces(splitAgg((String) row[10]))
                .workloads(splitAgg((String) row[11]))
                .build();
    }

    private static List<String> splitAgg(String agg) {
        return agg == null || agg.isEmpty() ? List.of() : List.of(agg.split("\\|"));
    }

    private Instant toInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to Instant");
    }

    private PackageSearchResult toSearchResult(SbomPackage pkg) {
        return PackageSearchResult.builder()
                .name(pkg.getName())
                .version(pkg.getVersion())
                .type(pkg.getType())
                .purl(pkg.getPurl())
                .license(pkg.getLicense())
                .imageReference(pkg.getImageReference())
                .build();
    }

    // Key on name + type so same-named packages of different types aren't collapsed.
    private Map<String, SbomPackage> indexByIdentity(List<SbomPackage> packages) {
        Map<String, SbomPackage> index = new LinkedHashMap<>();
        for (SbomPackage pkg : packages) {
            index.put(pkg.getName() + " " + (pkg.getType() == null ? "" : pkg.getType()), pkg);
        }
        return index;
    }

    private SbomDiffResult.PackageEntry toPackageEntry(SbomPackage pkg) {
        return SbomDiffResult.PackageEntry.builder()
                .name(pkg.getName())
                .version(pkg.getVersion())
                .type(pkg.getType())
                .license(pkg.getLicense())
                .build();
    }
}
