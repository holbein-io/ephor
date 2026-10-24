package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.ContainerData;
import io.holbein.ephor.api.dto.ScanIngestRequest;
import io.holbein.ephor.api.dto.WorkloadData;
import io.holbein.ephor.api.dto.sbom.*;
import io.holbein.ephor.api.dto.vulnerability.VulnerabilityData;
import io.holbein.ephor.api.entity.Vulnerability;
import io.holbein.ephor.api.entity.Workload;
import io.holbein.ephor.api.model.enums.ScanStatus;
import io.holbein.ephor.api.model.enums.SeverityLevel;
import io.holbein.ephor.api.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SbomQueryServiceTest extends BaseIntegrationTest {

    @Autowired private SbomQueryService sbomQueryService;
    @Autowired private SbomIngestionService sbomIngestionService;
    @Autowired private ScanIngestionService scanIngestionService;
    @Autowired private SbomPackageRepository sbomPackageRepository;
    @Autowired private SbomDocumentRepository sbomDocumentRepository;
    @Autowired private VulnerabilityInstanceRepository vulnerabilityInstanceRepository;
    @Autowired private ContainerRepository containerRepository;
    @Autowired private WorkloadRepository workloadRepository;
    @Autowired private ScanRepository scanRepository;
    @Autowired private VulnerabilityRepository vulnerabilityRepository;

    @BeforeEach
    void setUp() {
        sbomPackageRepository.deleteAll();
        sbomDocumentRepository.deleteAll();
        vulnerabilityInstanceRepository.deleteAll();
        containerRepository.deleteAll();
        workloadRepository.deleteAll();
        scanRepository.deleteAll();
        vulnerabilityRepository.deleteAll();
    }

    @Test
    void getMetadata_extractsPackageCountFromCycloneDx() {
        ingestCycloneDx("nginx:1.25", List.of(
                Map.of("name", "openssl", "version", "3.0.12"),
                Map.of("name", "zlib", "version", "1.2.13"),
                Map.of("name", "curl", "version", "8.5.0")
        ));

        Optional<SbomMetadata> metadata = sbomQueryService.getMetadata("nginx:1.25");

        assertThat(metadata).isPresent();
        assertThat(metadata.get().getPackageCount()).isEqualTo(3);
        assertThat(metadata.get().getFormat()).isEqualTo("cyclonedx");
        assertThat(metadata.get().getImageReference()).isEqualTo("nginx:1.25");
    }

    @Test
    void getCoverage_measuresDeployedImagesWithSbom() {
        // Two deployed images; only one has an SBOM ingested.
        ingestDeployedImage("nginx", "1.25");
        ingestDeployedImage("redis", "7.2");
        ingestCycloneDx("nginx:1.25", List.of(Map.of("name", "openssl", "version", "3.0.12")));

        SbomCoverageResponse coverage = sbomQueryService.getCoverage();

        // Denominator is the deployed fleet (2), numerator is deployed images that have an SBOM (1).
        assertThat(coverage.getTotalImages()).isEqualTo(2);
        assertThat(coverage.getImagesWithSbom()).isEqualTo(1);
        assertThat(coverage.getFormatBreakdown()).containsEntry("cyclonedx", 1L);
    }

    @Test
    void getCoverage_ignoresStaleSbomsForUndeployedImages() {
        // One deployed image with an SBOM, plus two SBOMs for images that are no longer deployed.
        ingestDeployedImage("nginx", "1.25");
        ingestCycloneDx("nginx:1.25", List.of(Map.of("name", "openssl", "version", "3.0.12")));
        ingestCycloneDx("redis:7.2", List.of(Map.of("name", "libc", "version", "2.36")));
        ingestCycloneDx("postgres:16", List.of(Map.of("name", "zlib", "version", "1.3")));

        SbomCoverageResponse coverage = sbomQueryService.getCoverage();

        // Stale SBOMs for undeployed images are not counted: deployed coverage is 1/1.
        assertThat(coverage.getTotalImages()).isEqualTo(1);
        assertThat(coverage.getImagesWithSbom()).isEqualTo(1);
    }

    private void ingestDeployedImage(String imageName, String imageTag) {
        ingestScannedImage(imageName, imageTag, List.of());
    }

    private void ingestScannedImage(String imageName, String imageTag, List<VulnerabilityData> vulnerabilities) {
        ScanIngestRequest scanRequest = new ScanIngestRequest();
        scanRequest.setNamespace("ns1");
        scanRequest.setScanLabel("scan-" + imageName);
        scanRequest.setStatus(ScanStatus.completed);
        scanRequest.setStartedAt(Instant.now());

        WorkloadData workload = new WorkloadData();
        workload.setNamespace("ns1");
        workload.setName(imageName + "-app");
        workload.setKind(Workload.WorkloadKind.Deployment);

        ContainerData container = new ContainerData();
        container.setName(imageName);
        container.setImageName(imageName);
        container.setImageTag(imageTag);
        container.setVulnerabilities(vulnerabilities);

        workload.setContainers(List.of(container));
        scanRequest.setWorkloads(List.of(workload));
        scanIngestionService.ingestScan(scanRequest);
    }

    @Test
    void diff_detectsAddedRemovedAndChangedPackages() {
        SbomIngestResponse v1 = ingestCycloneDx("nginx:1.25", List.of(
                Map.of("name", "openssl", "version", "3.0.11"),
                Map.of("name", "zlib", "version", "1.2.13"),
                Map.of("name", "curl", "version", "8.4.0")
        ));

        SbomIngestResponse v2 = ingestCycloneDx("nginx:1.25", List.of(
                Map.of("name", "openssl", "version", "3.0.12"),
                Map.of("name", "zlib", "version", "1.2.13"),
                Map.of("name", "libssl", "version", "3.0.12")
        ));

        SbomDiffResult diff = sbomQueryService.diff(v1.getId(), v2.getId());

        assertThat(diff.getImageReference()).isEqualTo("nginx:1.25");
        assertThat(diff.getAdded()).hasSize(1);
        assertThat(diff.getAdded().get(0).getName()).isEqualTo("libssl");
        assertThat(diff.getRemoved()).hasSize(1);
        assertThat(diff.getRemoved().get(0).getName()).isEqualTo("curl");
        assertThat(diff.getChanged()).hasSize(1);
        assertThat(diff.getChanged().get(0).getName()).isEqualTo("openssl");
        assertThat(diff.getChanged().get(0).getOldVersion()).isEqualTo("3.0.11");
        assertThat(diff.getChanged().get(0).getNewVersion()).isEqualTo("3.0.12");
        assertThat(diff.getUnchangedCount()).isEqualTo(1);
    }

    @Test
    void findUnreportedCves_flagsRunningImageWhereScannerDidNotReportKnownCve() {
        // The scanner found the CVE on nginx, scanned redis clean, yet redis ships the same openssl.
        ingestScannedImage("nginx", "1.25", List.of(knownCve("CVE-2025-0001", "openssl", "3.0.11", SeverityLevel.CRITICAL)));
        ingestScannedImage("redis", "7.2", List.of());
        ingestCycloneDx("redis:7.2", List.of(Map.of("name", "openssl", "version", "3.0.11")));

        Page<UnreportedCve> page = sbomQueryService.findUnreportedCves(noFilter(), PageRequest.of(0, 25));

        assertThat(page.getTotalElements()).isEqualTo(1);
        UnreportedCve cve = page.getContent().get(0);
        assertThat(cve.getCveId()).isEqualTo("CVE-2025-0001");
        assertThat(cve.getImageReference()).isEqualTo("redis:7.2");
        assertThat(cve.getSeverity()).isEqualTo("CRITICAL");
        assertThat(cve.getPackages()).containsExactly("openssl@3.0.11");
        assertThat(cve.getNamespaces()).containsExactly("ns1");
        assertThat(cve.getWorkloads()).containsExactly("ns1/redis-app");
        assertThat(cve.getVulnerabilityId()).isPositive();
    }

    @Test
    void findUnreportedCves_ignoresImageRunningPatchedVersionOfSamePackage() {
        ingestScannedImage("nginx", "1.25", List.of(knownCve("CVE-2025-0001", "openssl", "3.0.11", SeverityLevel.CRITICAL)));
        ingestScannedImage("redis", "7.2", List.of());
        // redis ships the PATCHED 3.0.12, so name-only matching must not flag it.
        ingestCycloneDx("redis:7.2", List.of(Map.of("name", "openssl", "version", "3.0.12")));

        assertThat(sbomQueryService.findUnreportedCves(noFilter(), PageRequest.of(0, 25)).getTotalElements()).isZero();
    }

    @Test
    void findUnreportedCves_usesLatestSbomNotStaleVulnerableVersion() {
        ingestScannedImage("nginx", "1.25", List.of(knownCve("CVE-2025-0001", "openssl", "3.0.11", SeverityLevel.CRITICAL)));
        ingestScannedImage("redis", "7.2", List.of());
        // redis shipped the vulnerable 3.0.11, then a newer SBOM with the patched 3.0.12.
        ingestCycloneDx("redis:7.2", List.of(Map.of("name", "openssl", "version", "3.0.11")));
        ingestCycloneDx("redis:7.2", List.of(Map.of("name", "openssl", "version", "3.0.12")));

        assertThat(sbomQueryService.findUnreportedCves(noFilter(), PageRequest.of(0, 25)).getTotalElements()).isZero();
    }

    @Test
    void findUnreportedCves_ignoresImagesNoLongerDeployed() {
        ingestScannedImage("nginx", "1.25", List.of(knownCve("CVE-2025-0001", "openssl", "3.0.11", SeverityLevel.CRITICAL)));
        // redis has an SBOM but no container runs it any more.
        ingestCycloneDx("redis:7.2", List.of(Map.of("name", "openssl", "version", "3.0.11")));

        assertThat(sbomQueryService.findUnreportedCves(noFilter(), PageRequest.of(0, 25)).getTotalElements()).isZero();
        assertThat(sbomQueryService.summarizeUnreportedCves().getTotal()).isZero();
    }

    @Test
    void findUnreportedCves_collapsesPackageRowsToOneRowPerCveAndImage() {
        // The same CVE is known against two package rows; redis carries both.
        ingestScannedImage("nginx", "1.25", List.of(
                knownCve("CVE-2025-0001", "openssl", "3.0.11", SeverityLevel.HIGH),
                knownCve("CVE-2025-0001", "libssl", "3.0.11", SeverityLevel.CRITICAL)));
        ingestScannedImage("redis", "7.2", List.of());
        ingestCycloneDx("redis:7.2", List.of(
                Map.of("name", "openssl", "version", "3.0.11"),
                Map.of("name", "libssl", "version", "3.0.11")));

        Page<UnreportedCve> page = sbomQueryService.findUnreportedCves(noFilter(), PageRequest.of(0, 25));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getPackages()).containsExactlyInAnyOrder("openssl@3.0.11", "libssl@3.0.11");
        assertThat(page.getContent().get(0).getSeverity()).isEqualTo("CRITICAL");
    }

    @Test
    void findUnreportedCves_ranksKevListedFirstAndSummarizes() {
        ingestScannedImage("nginx", "1.25", List.of(
                knownCve("CVE-2025-0001", "openssl", "3.0.11", SeverityLevel.CRITICAL),
                knownCve("CVE-2025-0002", "zlib", "1.2.13", SeverityLevel.HIGH)));
        Vulnerability kev = vulnerabilityRepository.findByNaturalKey("CVE-2025-0002", "zlib", "1.2.13", "trivy").orElseThrow();
        kev.setKevListed(true);
        vulnerabilityRepository.save(kev);
        ingestScannedImage("redis", "7.2", List.of());
        ingestCycloneDx("redis:7.2", List.of(
                Map.of("name", "openssl", "version", "3.0.11"),
                Map.of("name", "zlib", "version", "1.2.13")));

        Page<UnreportedCve> page = sbomQueryService.findUnreportedCves(noFilter(), PageRequest.of(0, 25));
        assertThat(page.getContent()).extracting(UnreportedCve::getCveId).containsExactly("CVE-2025-0002", "CVE-2025-0001");

        UnreportedCveSummary summary = sbomQueryService.summarizeUnreportedCves();
        assertThat(summary.getTotal()).isEqualTo(2);
        assertThat(summary.getKevListed()).isEqualTo(1);
        assertThat(summary.getImages()).isEqualTo(1);

        UnreportedCveFilter kevOnly = UnreportedCveFilter.builder().kevOnly(true).build();
        assertThat(sbomQueryService.findUnreportedCves(kevOnly, PageRequest.of(0, 25)).getTotalElements()).isEqualTo(1);
    }

    private static UnreportedCveFilter noFilter() {
        return UnreportedCveFilter.builder().build();
    }

    private static VulnerabilityData knownCve(String cveId, String packageName, String version, SeverityLevel severity) {
        VulnerabilityData vuln = new VulnerabilityData();
        vuln.setCveId(cveId);
        vuln.setPackageName(packageName);
        vuln.setPackageVersion(version);
        vuln.setSeverity(severity);
        vuln.setScannerType("trivy");
        vuln.setTitle(cveId + " in " + packageName);
        return vuln;
    }

    @Test
    void getLicenseDistribution_countsDistinctPackagesNotOccurrences() {
        // The same package (openssl 3.0.11, MIT) appears in three different images.
        ingestLicensed("nginx:1.25", List.of(licensedComponent("openssl", "3.0.11", "pkg:deb/openssl@3.0.11", "MIT")));
        ingestLicensed("redis:7.2", List.of(licensedComponent("openssl", "3.0.11", "pkg:deb/openssl@3.0.11", "MIT")));
        ingestLicensed("postgres:16", List.of(licensedComponent("openssl", "3.0.11", "pkg:deb/openssl@3.0.11", "MIT")));

        SbomPackageResponses.LicenseDistribution mit = sbomQueryService.getLicenseDistribution().stream()
                .filter(d -> "MIT".equals(d.getLicense()))
                .findFirst()
                .orElseThrow();

        // One distinct package, present in three images - not three "occurrences".
        assertThat(mit.getPackageCount()).isEqualTo(1);
        assertThat(mit.getImageCount()).isEqualTo(3);
    }

    private Map<String, Object> licensedComponent(String name, String version, String purl, String license) {
        return Map.of(
                "name", name,
                "version", version,
                "purl", purl,
                "licenses", List.of(Map.of("license", Map.of("id", license)))
        );
    }

    private SbomIngestResponse ingestLicensed(String imageReference, List<Map<String, Object>> components) {
        SbomIngestRequest request = new SbomIngestRequest();
        request.setImageReference(imageReference);
        request.setScanGroupId(UUID.randomUUID());
        request.setFormat("cyclonedx");
        request.setSbom(Map.of(
                "bomFormat", "CycloneDX",
                "specVersion", "1.5",
                "components", components
        ));
        return sbomIngestionService.ingest(request);
    }

    private SbomIngestResponse ingestCycloneDx(String imageReference, List<Map<String, String>> components) {
        SbomIngestRequest request = new SbomIngestRequest();
        request.setImageReference(imageReference);
        request.setScanGroupId(UUID.randomUUID());
        request.setFormat("cyclonedx");
        request.setSbom(Map.of(
                "bomFormat", "CycloneDX",
                "specVersion", "1.5",
                "components", components
        ));
        return sbomIngestionService.ingest(request);
    }
}
