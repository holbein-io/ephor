package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.ContainerData;
import io.holbein.ephor.api.dto.ScanIngestRequest;
import io.holbein.ephor.api.dto.ScanIngestResponse;
import io.holbein.ephor.api.dto.WorkloadData;
import io.holbein.ephor.api.dto.vulnerability.VulnerabilityData;
import io.holbein.ephor.api.entity.*;
import io.holbein.ephor.api.model.enums.ScanStatus;
import io.holbein.ephor.api.model.enums.SeverityLevel;
import io.holbein.ephor.api.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScanIngestionService {

    private final ScanRepository scanRepository;
    private final WorkloadRepository workloadRepository;
    private final ContainerRepository containerRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final VulnerabilityInstanceRepository vulnerabilityInstanceRepository;
    private final RemediationService remediationService;

    @Transactional
    public ScanIngestResponse ingestScan(ScanIngestRequest request) {
        log.info("Starting scan ingestion for namespace: {}, label: {}",
                request.getNamespace(), request.getScanLabel());

        Scan scan = Scan.builder()
                .namespace(request.getNamespace())
                .scanLabel(request.getScanLabel())
                .scanGroupId(request.getScanGroupId())
                .status(request.getStatus())
                .startedAt(request.getStartedAt() != null ? request.getStartedAt() : Instant.now())
                .trivyVersion(request.getTrivyVersion())
                .scanConfig(request.getScanConfig())
                .build();

        if (request.getCompletedAt() != null) {
            scan.setCompletedAt(request.getCompletedAt());
        }

        scan = scanRepository.save(scan);
        log.debug("Created scan with ID: {}", scan.getId());

        int totalVulnerabilities = 0;
        int criticalVulns = 0;
        int totalAutoResolved = 0;
        int totalReopened = 0;
        Set<Long> allAutoResolvedVulnIds = new HashSet<>();

        boolean skipAutoResolve = request.getStatus() == ScanStatus.failed;
        if (skipAutoResolve) {
            log.warn("Scan status is 'failed' for namespace: {} -- skipping auto-resolve",
                    request.getNamespace());
        }

        for (WorkloadData workloadData : request.getWorkloads()) {
            WorkloadProcessingResult result = processWorkload(scan, workloadData, skipAutoResolve);
            totalVulnerabilities += result.vulnerabilityCount;
            criticalVulns += result.criticalCount;
            totalAutoResolved += result.autoResolvedCount;
            totalReopened += result.reopenedCount;
            allAutoResolvedVulnIds.addAll(result.autoResolvedVulnIds);
        }

        // Auto-complete any active remediations for auto-resolved vulnerabilities
        remediationService.autoCompleteRemediationsForVulnerabilities(allAutoResolvedVulnIds);

        log.info("Scan ingestion complete. Scan ID: {}, Workloads: {}, Vulnerabilities: {}, Critical: {}, Auto-resolved: {}, Reopened: {}",
                scan.getId(), request.getWorkloads().size(), totalVulnerabilities, criticalVulns, totalAutoResolved, totalReopened);

        return ScanIngestResponse.builder()
                .scanId(scan.getId())
                .workloads(request.getWorkloads().size())
                .vulnerabilities(totalVulnerabilities)
                .criticalVulns(criticalVulns)
                .autoResolved(totalAutoResolved)
                .reopened(totalReopened)
                .build();
    }

    private WorkloadProcessingResult processWorkload(Scan scan, WorkloadData workloadData, boolean skipAutoResolve) {
        // Upsert workload
        Workload workload = workloadRepository.findByNaturalKey(
                workloadData.getNamespace(),
                workloadData.getName(),
                workloadData.getKind()
        ).orElse(null);

        if (workload == null) {
            workload = Workload.builder()
                    .scan(scan)
                    .namespace(workloadData.getNamespace())
                    .name(workloadData.getName())
                    .kind(workloadData.getKind())
                    .lastScan(scan)
                    .build();
        } else {
            workload.setLastScan(scan);
        }

        workload = workloadRepository.save(workload);

        syncWorkloadLabels(workload, workloadData.getLabels());

        int vulnerabilityCount = 0;
        int criticalCount = 0;
        int autoResolvedCount = 0;
        int reopenedCount = 0;
        Set<Long> autoResolvedVulnIds = new HashSet<>();

        for (ContainerData containerData : workloadData.getContainers()) {
            ContainerProcessingResult result = processContainer(scan, workload, containerData, skipAutoResolve);
            vulnerabilityCount += result.vulnerabilityCount;
            criticalCount += result.criticalCount;
            autoResolvedCount += result.autoResolvedCount;
            reopenedCount += result.reopenedCount;
            autoResolvedVulnIds.addAll(result.autoResolvedVulnIds);
        }

        return new WorkloadProcessingResult(vulnerabilityCount, criticalCount, autoResolvedCount, reopenedCount, autoResolvedVulnIds);
    }

    private ContainerProcessingResult processContainer(Scan scan, Workload workload, ContainerData containerData, boolean skipAutoResolve) {
        // Upsert container
        Container container = containerRepository.findByWorkloadIdAndName(workload.getId(), containerData.getName())
                .orElse(null);

        if (container == null) {
            container = Container.builder()
                    .workload(workload)
                    .name(containerData.getName())
                    .imageName(containerData.getImageName())
                    .imageTag(containerData.getImageTag())
                    .imageCreated(containerData.getImageCreated())
                    .baseImageCreated(containerData.getBaseImageCreated())
                    .detectedEcosystems(containerData.getDetectedEcosystems())
                    .osFamily(containerData.getOsFamily())
                    .osName(containerData.getOsName())
                    .repoDigests(containerData.getRepoDigests())
                    .build();
        } else {
            if (containerData.getImageName() != null) {
                container.setImageName(containerData.getImageName());
            }
            if (containerData.getImageTag() != null) {
                container.setImageTag(containerData.getImageTag());
            }
            if (containerData.getImageCreated() != null) {
                container.setImageCreated(containerData.getImageCreated());
            }
            if (containerData.getBaseImageCreated() != null) {
                container.setBaseImageCreated(containerData.getBaseImageCreated());
            }
            if (containerData.getDetectedEcosystems() != null) {
                container.setDetectedEcosystems(containerData.getDetectedEcosystems());
            }
            if (containerData.getOsFamily() != null) {
                container.setOsFamily(containerData.getOsFamily());
            }
            if (containerData.getOsName() != null) {
                container.setOsName(containerData.getOsName());
            }
            if (containerData.getRepoDigests() != null) {
                container.setRepoDigests(containerData.getRepoDigests());
            }
        }

        container = containerRepository.save(container);

        int vulnerabilityCount = 0;
        int criticalCount = 0;
        int reopenedCount = 0;
        List<Long> currentVulnerabilityIds = new ArrayList<>();

        if (containerData.getVulnerabilities() != null) {
            for (VulnerabilityData vulnData : containerData.getVulnerabilities()) {
                VulnerabilityProcessingResult result = processVulnerability(scan, container, vulnData);
                currentVulnerabilityIds.add(result.vulnerabilityId);
                vulnerabilityCount++;

                if (vulnData.getSeverity() == SeverityLevel.CRITICAL) {
                    criticalCount++;
                }

                if (result.reopened) {
                    reopenedCount++;
                }
            }
        }

        List<Long> autoResolvedVulnIds = List.of();
        int autoResolvedCount = 0;

        if (!skipAutoResolve) {
            if (currentVulnerabilityIds.isEmpty()) {
                autoResolvedVulnIds = vulnerabilityInstanceRepository.findAllOpenVulnerabilityIdsForContainer(container.getId());
            } else {
                autoResolvedVulnIds = vulnerabilityInstanceRepository.findVulnerabilityIdsToAutoResolve(container.getId(), currentVulnerabilityIds);
            }

            autoResolvedCount = autoResolveVulnerabilities(container.getId(), scan.getId(), currentVulnerabilityIds);

            if (autoResolvedCount > 0) {
                log.info("Auto-resolved {} vulnerability instances for container {} in workload {}/{}",
                        autoResolvedCount, container.getName(), workload.getNamespace(), workload.getName());
            }
        }

        return new ContainerProcessingResult(vulnerabilityCount, criticalCount, autoResolvedCount, reopenedCount, new HashSet<>(autoResolvedVulnIds));
    }

    private VulnerabilityProcessingResult processVulnerability(Scan scan, Container container, VulnerabilityData vulnData) {
        Instant now = Instant.now();

        // Upsert vulnerability
        Vulnerability vulnerability = vulnerabilityRepository.findByNaturalKey(
                vulnData.getCveId(),
                vulnData.getPackageName(),
                vulnData.getPackageVersion(),
                vulnData.getScannerType()
        ).orElse(null);

        if (vulnerability == null) {
            vulnerability = Vulnerability.builder()
                    .cveId(vulnData.getCveId())
                    .packageName(vulnData.getPackageName())
                    .packageVersion(vulnData.getPackageVersion())
                    .severity(vulnData.getSeverity())
                    .title(vulnData.getTitle())
                    .description(vulnData.getDescription())
                    .primaryUrl(vulnData.getPrimaryUrl())
                    .publishedDate(vulnData.getPublishedDate())
                    .fixedVersion(vulnData.getFixedVersion())
                    .scannerType(vulnData.getScannerType())
                    .packageClass(vulnData.getPackageClass())
                    .packageType(vulnData.getPackageType())
                    .references(vulnData.getReferences())
                    .cvssV3Vector(vulnData.getCvssV3Vector())
                    .cvssV3Score(vulnData.getCvssV3Score())
                    .firstDetected(vulnData.getFirstDetected() != null ? vulnData.getFirstDetected() : now)
                    .lastSeen(now)
                    .build();
        } else {
            vulnerability.setLastSeen(now);
            if (vulnData.getTitle() != null) {
                vulnerability.setTitle(vulnData.getTitle());
            }
            if (vulnData.getDescription() != null) {
                vulnerability.setDescription(vulnData.getDescription());
            }
            if (vulnData.getPrimaryUrl() != null) {
                vulnerability.setPrimaryUrl(vulnData.getPrimaryUrl());
            }
            if (vulnData.getPublishedDate() != null) {
                vulnerability.setPublishedDate(vulnData.getPublishedDate());
            }
            if (vulnData.getFixedVersion() != null) {
                vulnerability.setFixedVersion(vulnData.getFixedVersion());
            }
            if (vulnData.getPackageClass() != null) {
                vulnerability.setPackageClass(vulnData.getPackageClass());
            }
            if (vulnData.getPackageType() != null) {
                vulnerability.setPackageType(vulnData.getPackageType());
            }
            if (vulnData.getReferences() != null) {
                vulnerability.setReferences(vulnData.getReferences());
            }
            if (vulnData.getCvssV3Vector() != null) {
                vulnerability.setCvssV3Vector(vulnData.getCvssV3Vector());
            }
            if (vulnData.getCvssV3Score() != null) {
                vulnerability.setCvssV3Score(vulnData.getCvssV3Score());
            }
        }

        vulnerability = vulnerabilityRepository.save(vulnerability);

        // Check if instance already exists for this container
        boolean reopened = false;
        Optional<VulnerabilityInstance> existingInstance = vulnerabilityInstanceRepository
                .findByVulnerabilityIdAndContainerId(vulnerability.getId(), container.getId());

        if (existingInstance.isPresent()) {
            VulnerabilityInstance instance = existingInstance.get();
            if (instance.getStatus() == VulnerabilityInstance.InstanceStatus.resolved) {
                int updated = vulnerabilityInstanceRepository.reopenResolvedForContainer(
                        container.getId(), vulnerability.getId(), scan.getId());
                if (updated > 0) {
                    reopened = true;
                    log.debug("Reopened vulnerability instance: {} for container {} in workload {}/{}",
                            vulnerability.getCveId(), container.getName(),
                            container.getWorkload().getNamespace(), container.getWorkload().getName());
                }
            }
        } else {
            VulnerabilityInstance instance = VulnerabilityInstance.builder()
                    .vulnerability(vulnerability)
                    .container(container)
                    .scan(scan)
                    .status(VulnerabilityInstance.InstanceStatus.open)
                    .build();
            vulnerabilityInstanceRepository.save(instance);
        }

        return new VulnerabilityProcessingResult(vulnerability.getId(), reopened);
    }

    private int autoResolveVulnerabilities(Long containerId, Long scanId, List<Long> currentVulnerabilityIds) {
        String reason = String.format("Auto-resolved: Vulnerability no longer detected in scan %d", scanId);

        if (currentVulnerabilityIds.isEmpty()) {
            return vulnerabilityInstanceRepository.autoResolveAllForContainer(containerId, scanId, reason);
        }

        return vulnerabilityInstanceRepository.autoResolveForContainer(
                containerId, scanId, currentVulnerabilityIds, reason);
    }

    private record WorkloadProcessingResult(int vulnerabilityCount, int criticalCount, int autoResolvedCount,
                                            int reopenedCount, Set<Long> autoResolvedVulnIds) {
    }

    private record ContainerProcessingResult(int vulnerabilityCount, int criticalCount, int autoResolvedCount,
                                             int reopenedCount, Set<Long> autoResolvedVulnIds) {
    }

    private record VulnerabilityProcessingResult(Long vulnerabilityId, boolean reopened) {
    }

    private void syncWorkloadLabels(Workload workload, Map<String, String> labels) {
        workload.getLabels().clear();
        workloadRepository.flush();

        if (labels != null && !labels.isEmpty()) {
            for (Map.Entry<String, String> entry : labels.entrySet()) {
                WorkloadLabel label = WorkloadLabel.builder()
                        .workload(workload)
                        .labelKey(entry.getKey())
                        .labelValue(entry.getValue())
                        .build();
                workload.getLabels().add(label);
            }
            workloadRepository.save(workload);
        }
    }
}
