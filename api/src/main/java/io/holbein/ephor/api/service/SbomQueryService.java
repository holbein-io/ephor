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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        long totalImages = containerRepository.countDistinctImages();
        long imagesWithSbom = sbomDocumentRepository.countDistinctImageReferences();

        Map<String, Long> formatBreakdown = new LinkedHashMap<>();
        for (Object[] row : sbomDocumentRepository.countByFormat()) {
            formatBreakdown.put((String) row[0], (Long) row[1]);
        }

        return SbomCoverageResponse.builder()
                .totalImages(totalImages)
                .imagesWithSbom(imagesWithSbom)
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

    public Page<TopPackageEntry> getTopPackages(Pageable pageable) {
        return sbomPackageRepository.findTopPackages(pageable)
                .map(row -> TopPackageEntry.builder()
                        .name((String) row[0])
                        .version((String) row[1])
                        .type((String) row[2])
                        .imageCount((Long) row[3])
                        .build());
    }

    public List<LicenseDistributionEntry> getLicenseDistribution() {
        return sbomPackageRepository.getLicenseDistribution().stream()
                .map(row -> LicenseDistributionEntry.builder()
                        .license((String) row[0])
                        .packageCount((Long) row[1])
                        .imageCount((Long) row[2])
                        .build())
                .toList();
    }

    public Page<PackageSearchResult> findByLicense(String license, Pageable pageable) {
        return sbomPackageRepository.findByLicense(license, pageable)
                .map(this::toSearchResult);
    }

    // --- Diff ---

    public SbomDiffResult diff(UUID sbomIdA, UUID sbomIdB) {
        sbomDocumentRepository.findById(sbomIdA)
                .orElseThrow(() -> new ResourceNotFoundException(ProblemType.RESOURCE_NOT_FOUND,
                        "SBOM document not found: " + sbomIdA));
        SbomDocument docB = sbomDocumentRepository.findById(sbomIdB)
                .orElseThrow(() -> new ResourceNotFoundException(ProblemType.RESOURCE_NOT_FOUND,
                        "SBOM document not found: " + sbomIdB));

        Map<String, SbomPackage> packagesA = indexByName(sbomPackageRepository.findBySbomDocumentId(sbomIdA));
        Map<String, SbomPackage> packagesB = indexByName(sbomPackageRepository.findBySbomDocumentId(sbomIdB));

        List<PackageDiff> added = new ArrayList<>();
        List<PackageDiff> removed = new ArrayList<>();
        List<PackageChangeDiff> changed = new ArrayList<>();
        int unchanged = 0;

        for (Map.Entry<String, SbomPackage> entry : packagesB.entrySet()) {
            SbomPackage pkgB = entry.getValue();
            SbomPackage pkgA = packagesA.get(entry.getKey());

            if (pkgA == null) {
                added.add(toPackageDiff(pkgB));
            } else if (!pkgA.getVersion().equals(pkgB.getVersion())) {
                changed.add(PackageChangeDiff.builder()
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
                removed.add(toPackageDiff(entry.getValue()));
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

    // --- Pre-scan alerts (CVE matching) ---

    public List<PreScanAlert> findPreScanAlerts(int limit) {
        Query query = entityManager.createNativeQuery("""
            SELECT DISTINCT v.cve_id, v.severity, v.package_name, v.package_version, v.title,
                   sp.image_reference, sp.version as sbom_package_version
            FROM vulnerabilities v
            JOIN sbom_packages sp ON sp.name = v.package_name
            WHERE v.severity IN ('CRITICAL', 'HIGH')
              AND NOT EXISTS (
                SELECT 1
                FROM vulnerability_instances vi
                JOIN containers c ON c.id = vi.container_id
                WHERE vi.vulnerability_id = v.id
                  AND CONCAT(c.image_name, ':', c.image_tag) = sp.image_reference
              )
            ORDER BY v.severity, v.cve_id
            """);
        query.setMaxResults(limit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        return rows.stream()
                .map(row -> PreScanAlert.builder()
                        .cveId((String) row[0])
                        .severity((String) row[1])
                        .packageName((String) row[2])
                        .packageVersion((String) row[3])
                        .title((String) row[4])
                        .imageReference((String) row[5])
                        .sbomPackageVersion((String) row[6])
                        .build())
                .toList();
    }

    public long countPreScanAlerts() {
        Query query = entityManager.createNativeQuery("""
            SELECT COUNT(DISTINCT (v.cve_id, sp.image_reference))
            FROM vulnerabilities v
            JOIN sbom_packages sp ON sp.name = v.package_name
            WHERE v.severity IN ('CRITICAL', 'HIGH')
              AND NOT EXISTS (
                SELECT 1
                FROM vulnerability_instances vi
                JOIN containers c ON c.id = vi.container_id
                WHERE vi.vulnerability_id = v.id
                  AND CONCAT(c.image_name, ':', c.image_tag) = sp.image_reference
              )
            """);
        return ((Number) query.getSingleResult()).longValue();
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

    private Map<String, SbomPackage> indexByName(List<SbomPackage> packages) {
        Map<String, SbomPackage> index = new LinkedHashMap<>();
        for (SbomPackage pkg : packages) {
            index.put(pkg.getName(), pkg);
        }
        return index;
    }

    private PackageDiff toPackageDiff(SbomPackage pkg) {
        return PackageDiff.builder()
                .name(pkg.getName())
                .version(pkg.getVersion())
                .type(pkg.getType())
                .license(pkg.getLicense())
                .build();
    }
}
