package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.ContainerData;
import io.holbein.ephor.api.dto.ScanIngestRequest;
import io.holbein.ephor.api.dto.WorkloadData;
import io.holbein.ephor.api.dto.sbom.*;
import io.holbein.ephor.api.dto.vulnerability.VulnerabilityData;
import io.holbein.ephor.api.entity.Workload;
import io.holbein.ephor.api.model.enums.ScanStatus;
import io.holbein.ephor.api.model.enums.SeverityLevel;
import io.holbein.ephor.api.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
    void getCoverage_returnsCorrectCounts() {
        ingestCycloneDx("nginx:1.25", List.of(Map.of("name", "openssl", "version", "3.0.12")));
        ingestCycloneDx("redis:7.2", List.of(Map.of("name", "libc", "version", "2.36")));

        SbomCoverageResponse coverage = sbomQueryService.getCoverage();

        assertThat(coverage.getImagesWithSbom()).isEqualTo(2);
        assertThat(coverage.getFormatBreakdown()).containsEntry("cyclonedx", 2L);
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
    void findPreScanAlerts_detectsUnscannedImageWithVulnerablePackage() {
        ScanIngestRequest scanRequest = new ScanIngestRequest();
        scanRequest.setNamespace("ns1");
        scanRequest.setScanLabel("scan-1");
        scanRequest.setStatus(ScanStatus.completed);
        scanRequest.setStartedAt(Instant.now());

        WorkloadData workload = new WorkloadData();
        workload.setNamespace("ns1");
        workload.setName("web-app");
        workload.setKind(Workload.WorkloadKind.Deployment);

        ContainerData container = new ContainerData();
        container.setName("nginx");
        container.setImageName("nginx");
        container.setImageTag("1.25");

        VulnerabilityData vuln = new VulnerabilityData();
        vuln.setCveId("CVE-2025-0001");
        vuln.setPackageName("openssl");
        vuln.setPackageVersion("3.0.11");
        vuln.setSeverity(SeverityLevel.CRITICAL);
        vuln.setScannerType("trivy");
        vuln.setTitle("OpenSSL buffer overflow");

        container.setVulnerabilities(List.of(vuln));
        workload.setContainers(List.of(container));
        scanRequest.setWorkloads(List.of(workload));
        scanIngestionService.ingestScan(scanRequest);

        SbomIngestRequest sbomRequest = new SbomIngestRequest();
        sbomRequest.setImageReference("redis:7.2");
        sbomRequest.setScanGroupId(UUID.randomUUID());
        sbomRequest.setFormat("cyclonedx");
        sbomRequest.setSbom(Map.of(
                "bomFormat", "CycloneDX",
                "specVersion", "1.5",
                "components", List.of(
                        Map.of("name", "openssl", "version", "3.0.11", "purl", "pkg:debian/openssl@3.0.11")
                )
        ));
        sbomIngestionService.ingest(sbomRequest);

        List<PreScanAlert> alerts = sbomQueryService.findPreScanAlerts(50);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getCveId()).isEqualTo("CVE-2025-0001");
        assertThat(alerts.get(0).getImageReference()).isEqualTo("redis:7.2");
        assertThat(alerts.get(0).getPackageName()).isEqualTo("openssl");
        assertThat(sbomQueryService.countPreScanAlerts()).isEqualTo(1);
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
