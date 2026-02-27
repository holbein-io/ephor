package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.ContainerData;
import io.holbein.ephor.api.dto.ScanIngestRequest;
import io.holbein.ephor.api.dto.ScanIngestResponse;
import io.holbein.ephor.api.dto.WorkloadData;
import io.holbein.ephor.api.dto.vulnerability.VulnerabilityData;
import io.holbein.ephor.api.entity.VulnerabilityInstance;
import io.holbein.ephor.api.entity.Workload;
import io.holbein.ephor.api.model.enums.ScanStatus;
import io.holbein.ephor.api.model.enums.SeverityLevel;
import io.holbein.ephor.api.repositories.ContainerRepository;
import io.holbein.ephor.api.repositories.ScanRepository;
import io.holbein.ephor.api.repositories.VulnerabilityInstanceRepository;
import io.holbein.ephor.api.repositories.VulnerabilityRepository;
import io.holbein.ephor.api.repositories.WorkloadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScanIngestionServiceTest extends BaseIntegrationTest {

    @Autowired
    private ScanIngestionService scanIngestionService;

    @Autowired
    private ScanRepository scanRepository;

    @Autowired
    private WorkloadRepository workloadRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private VulnerabilityRepository vulnerabilityRepository;

    @Autowired
    private VulnerabilityInstanceRepository vulnerabilityInstanceRepository;

    @BeforeEach
    void setUp() {
        vulnerabilityInstanceRepository.deleteAll();
        containerRepository.deleteAll();
        workloadRepository.deleteAll();
        scanRepository.deleteAll();
        vulnerabilityRepository.deleteAll();
    }

    @Test
    void basicIngestion_createsScanWorkloadContainerAndVulnerability() {
        ScanIngestRequest request = buildRequest("ns1", "scan-1",
                List.of(buildWorkload("ns1", "web-app", Workload.WorkloadKind.Deployment,
                        List.of(buildContainer("nginx", "docker.io/library/nginx", "latest",
                                List.of(buildVuln("CVE-2025-0001", "openssl", "3.0.1", SeverityLevel.CRITICAL)))))));

        ScanIngestResponse response = scanIngestionService.ingestScan(request);

        assertThat(response.getScanId()).isNotNull();
        assertThat(response.getWorkloads()).isEqualTo(1);
        assertThat(response.getVulnerabilities()).isEqualTo(1);
        assertThat(response.getCriticalVulns()).isEqualTo(1);
        assertThat(response.getAutoResolved()).isZero();
        assertThat(response.getReopened()).isZero();

        assertThat(scanRepository.count()).isEqualTo(1);
        assertThat(workloadRepository.count()).isEqualTo(1);
        assertThat(containerRepository.count()).isEqualTo(1);
        assertThat(vulnerabilityRepository.count()).isEqualTo(1);
        assertThat(vulnerabilityInstanceRepository.count()).isEqualTo(1);
    }

    @Test
    void multiContainerIngestion_createsMultipleContainers() {
        ScanIngestRequest request = buildRequest("ns1", "scan-1",
                List.of(buildWorkload("ns1", "web-app", Workload.WorkloadKind.Deployment,
                        List.of(
                                buildContainer("nginx", "nginx", "1.25",
                                        List.of(buildVuln("CVE-2025-0001", "openssl", "3.0.1", SeverityLevel.CRITICAL))),
                                buildContainer("php-fpm", "php-fpm", "8.3",
                                        List.of(buildVuln("CVE-2025-0002", "curl", "7.88", SeverityLevel.HIGH)))
                        ))));

        ScanIngestResponse response = scanIngestionService.ingestScan(request);

        assertThat(response.getWorkloads()).isEqualTo(1);
        assertThat(response.getVulnerabilities()).isEqualTo(2);
        assertThat(workloadRepository.count()).isEqualTo(1);
        assertThat(containerRepository.count()).isEqualTo(2);
        assertThat(vulnerabilityInstanceRepository.count()).isEqualTo(2);
    }

    @Test
    void workloadDedup_reuseExistingWorkloadAndContainer() {
        ScanIngestRequest req1 = buildRequest("ns1", "scan-1",
                List.of(buildWorkload("ns1", "web-app", Workload.WorkloadKind.Deployment,
                        List.of(buildContainer("nginx", "nginx", "1.25",
                                List.of(buildVuln("CVE-2025-0001", "openssl", "3.0.1", SeverityLevel.HIGH)))))));
        scanIngestionService.ingestScan(req1);

        ScanIngestRequest req2 = buildRequest("ns1", "scan-2",
                List.of(buildWorkload("ns1", "web-app", Workload.WorkloadKind.Deployment,
                        List.of(buildContainer("nginx", "nginx", "1.25",
                                List.of(buildVuln("CVE-2025-0002", "curl", "7.88", SeverityLevel.MEDIUM)))))));
        scanIngestionService.ingestScan(req2);

        assertThat(workloadRepository.count()).isEqualTo(1);
        assertThat(containerRepository.count()).isEqualTo(1);
        assertThat(scanRepository.count()).isEqualTo(2);
        assertThat(vulnerabilityRepository.count()).isEqualTo(2);
        assertThat(vulnerabilityInstanceRepository.count()).isEqualTo(2);
    }

    @Test
    void vulnerabilityDedup_reuseExistingVulnerability() {
        ScanIngestRequest req1 = buildRequest("ns1", "scan-1",
                List.of(buildWorkload("ns1", "web-app", Workload.WorkloadKind.Deployment,
                        List.of(buildContainer("nginx", "nginx", "1.25",
                                List.of(buildVuln("CVE-2025-0001", "openssl", "3.0.1", SeverityLevel.CRITICAL)))))));
        scanIngestionService.ingestScan(req1);

        ScanIngestRequest req2 = buildRequest("ns1", "scan-2",
                List.of(buildWorkload("ns1", "api-server", Workload.WorkloadKind.Deployment,
                        List.of(buildContainer("app", "api-server", "latest",
                                List.of(buildVuln("CVE-2025-0001", "openssl", "3.0.1", SeverityLevel.CRITICAL)))))));
        scanIngestionService.ingestScan(req2);

        assertThat(vulnerabilityRepository.count()).isEqualTo(1);
        assertThat(vulnerabilityInstanceRepository.count()).isEqualTo(2);
    }

    @Test
    void autoResolve_resolvesMissingVulnerabilities() {
        ScanIngestRequest req1 = buildRequest("ns1", "scan-1",
                List.of(buildWorkload("ns1", "web-app", Workload.WorkloadKind.Deployment,
                        List.of(buildContainer("nginx", "nginx", "1.25",
                                List.of(
                                        buildVuln("CVE-2025-0001", "openssl", "3.0.1", SeverityLevel.CRITICAL),
                                        buildVuln("CVE-2025-0002", "curl", "7.88", SeverityLevel.HIGH)
                                ))))));
        scanIngestionService.ingestScan(req1);

        ScanIngestRequest req2 = buildRequest("ns1", "scan-2",
                List.of(buildWorkload("ns1", "web-app", Workload.WorkloadKind.Deployment,
                        List.of(buildContainer("nginx", "nginx", "1.25",
                                List.of(buildVuln("CVE-2025-0001", "openssl", "3.0.1", SeverityLevel.CRITICAL)))))));
        ScanIngestResponse response = scanIngestionService.ingestScan(req2);

        assertThat(response.getAutoResolved()).isEqualTo(1);

        List<VulnerabilityInstance> resolved = vulnerabilityInstanceRepository
                .findAllByStatus(VulnerabilityInstance.InstanceStatus.resolved);
        assertThat(resolved).hasSize(1);

        List<VulnerabilityInstance> open = vulnerabilityInstanceRepository
                .findAllByStatus(VulnerabilityInstance.InstanceStatus.open);
        assertThat(open).hasSize(1);
    }

    @Test
    void autoResolve_perContainer_independentResolution() {
        // Two containers in one workload, each with one vulnerability
        ScanIngestRequest req1 = buildRequest("ns1", "scan-1",
                List.of(buildWorkload("ns1", "web-app", Workload.WorkloadKind.Deployment,
                        List.of(
                                buildContainer("nginx", "nginx", "1.25",
                                        List.of(buildVuln("CVE-2025-0001", "openssl", "3.0.1", SeverityLevel.CRITICAL))),
                                buildContainer("php-fpm", "php-fpm", "8.3",
                                        List.of(buildVuln("CVE-2025-0003", "zlib", "1.2.13", SeverityLevel.MEDIUM)))
                        ))));
        scanIngestionService.ingestScan(req1);

        // Second scan: nginx vuln gone, php-fpm vuln remains
        ScanIngestRequest req2 = buildRequest("ns1", "scan-2",
                List.of(buildWorkload("ns1", "web-app", Workload.WorkloadKind.Deployment,
                        List.of(
                                buildContainer("nginx", "nginx", "1.26", List.of()),
                                buildContainer("php-fpm", "php-fpm", "8.3",
                                        List.of(buildVuln("CVE-2025-0003", "zlib", "1.2.13", SeverityLevel.MEDIUM)))
                        ))));
        ScanIngestResponse response = scanIngestionService.ingestScan(req2);

        assertThat(response.getAutoResolved()).isEqualTo(1);

        List<VulnerabilityInstance> resolved = vulnerabilityInstanceRepository
                .findAllByStatus(VulnerabilityInstance.InstanceStatus.resolved);
        assertThat(resolved).hasSize(1);

        List<VulnerabilityInstance> open = vulnerabilityInstanceRepository
                .findAllByStatus(VulnerabilityInstance.InstanceStatus.open);
        assertThat(open).hasSize(1);
    }

    @Test
    void reopen_reopensAutoResolvedVulnerability() {
        ScanIngestRequest req1 = buildRequest("ns1", "scan-1",
                List.of(buildWorkload("ns1", "web-app", Workload.WorkloadKind.Deployment,
                        List.of(buildContainer("nginx", "nginx", "1.25",
                                List.of(buildVuln("CVE-2025-0001", "openssl", "3.0.1", SeverityLevel.CRITICAL)))))));
        scanIngestionService.ingestScan(req1);

        ScanIngestRequest req2 = buildRequest("ns1", "scan-2",
                List.of(buildWorkload("ns1", "web-app", Workload.WorkloadKind.Deployment,
                        List.of(buildContainer("nginx", "nginx", "1.26", List.of())))));
        ScanIngestResponse resp2 = scanIngestionService.ingestScan(req2);
        assertThat(resp2.getAutoResolved()).isEqualTo(1);

        ScanIngestRequest req3 = buildRequest("ns1", "scan-3",
                List.of(buildWorkload("ns1", "web-app", Workload.WorkloadKind.Deployment,
                        List.of(buildContainer("nginx", "nginx", "1.25",
                                List.of(buildVuln("CVE-2025-0001", "openssl", "3.0.1", SeverityLevel.CRITICAL)))))));
        ScanIngestResponse resp3 = scanIngestionService.ingestScan(req3);
        assertThat(resp3.getReopened()).isEqualTo(1);

        List<VulnerabilityInstance> open = vulnerabilityInstanceRepository
                .findAllByStatus(VulnerabilityInstance.InstanceStatus.open);
        assertThat(open).hasSize(1);
    }

    private ScanIngestRequest buildRequest(String namespace, String scanLabel, List<WorkloadData> workloads) {
        ScanIngestRequest request = new ScanIngestRequest();
        request.setNamespace(namespace);
        request.setScanLabel(scanLabel);
        request.setStatus(ScanStatus.completed);
        request.setStartedAt(Instant.now().minusSeconds(60));
        request.setCompletedAt(Instant.now());
        request.setTrivyVersion("0.52.0");
        request.setWorkloads(workloads);
        return request;
    }

    private WorkloadData buildWorkload(String namespace, String name, Workload.WorkloadKind kind,
                                       List<ContainerData> containers) {
        WorkloadData wd = new WorkloadData();
        wd.setNamespace(namespace);
        wd.setName(name);
        wd.setKind(kind);
        wd.setContainers(containers);
        return wd;
    }

    private ContainerData buildContainer(String name, String imageName, String imageTag,
                                          List<VulnerabilityData> vulns) {
        ContainerData cd = new ContainerData();
        cd.setName(name);
        cd.setImageName(imageName);
        cd.setImageTag(imageTag);
        cd.setVulnerabilities(vulns);
        return cd;
    }

    private VulnerabilityData buildVuln(String cveId, String packageName, String packageVersion,
                                        SeverityLevel severity) {
        VulnerabilityData vd = new VulnerabilityData();
        vd.setCveId(cveId);
        vd.setPackageName(packageName);
        vd.setPackageVersion(packageVersion);
        vd.setSeverity(severity);
        vd.setScannerType("trivy");
        vd.setTitle(cveId + " in " + packageName);
        return vd;
    }
}
