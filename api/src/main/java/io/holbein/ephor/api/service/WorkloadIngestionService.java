package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.ContainerData;
import io.holbein.ephor.api.dto.WorkloadData;
import io.holbein.ephor.api.dto.vulnerability.VulnerabilityData;
import io.holbein.ephor.api.entity.*;
import io.holbein.ephor.api.model.enums.SeverityLevel;
import io.holbein.ephor.api.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

import static io.holbein.ephor.api.service.VulnerabilityFieldGuard.CVSS_V3_VECTOR;
import static io.holbein.ephor.api.service.VulnerabilityFieldGuard.PACKAGE_CLASS;
import static io.holbein.ephor.api.service.VulnerabilityFieldGuard.PACKAGE_TYPE;
import static io.holbein.ephor.api.service.VulnerabilityFieldGuard.truncate;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkloadIngestionService {

    private static final Duration LAST_SEEN_REFRESH_INTERVAL = Duration.ofHours(1);

    private final ScanRepository scanRepository;
    private final WorkloadRepository workloadRepository;
    private final ContainerRepository containerRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final VulnerabilityInstanceRepository vulnerabilityInstanceRepository;

    @Transactional
    public WorkloadIngestResult ingestWorkload(long scanId, WorkloadData workloadData, boolean skipAutoResolve) {
        Instant now = Instant.now();
        Scan scan = scanRepository.getReferenceById(scanId);

        Workload workload = upsertWorkload(scan, workloadData);
        syncWorkloadLabels(workload, workloadData.getLabels());

        ResolvedVulnerabilities resolved = resolveVulnerabilities(workloadData, now);

        int vulnerabilityCount = 0;
        int criticalCount = 0;
        int autoResolvedCount = 0;
        int reopenedCount = 0;
        Set<Long> autoResolvedVulnIds = new HashSet<>();

        for (ContainerData containerData : workloadData.getContainers()) {
            Container container = upsertContainer(workload, containerData);
            Set<Long> currentVulnerabilityIds = new LinkedHashSet<>();

            if (containerData.getVulnerabilities() != null) {
                for (VulnerabilityData vulnData : containerData.getVulnerabilities()) {
                    Vulnerability vulnerability = resolved.byKey().get(VulnerabilityKey.of(vulnData));
                    if (vulnerability == null) {
                        continue; // rejected during resolution, already logged and counted
                    }

                    currentVulnerabilityIds.add(vulnerability.getId());
                    vulnerabilityCount++;

                    if (vulnData.getSeverity() == SeverityLevel.CRITICAL) {
                        criticalCount++;
                    }
                    if (openOrReopenInstance(scan, container, vulnerability)) {
                        reopenedCount++;
                    }
                }
            }

            if (skipAutoResolve) {
                continue;
            }

            List<Long> currentIds = new ArrayList<>(currentVulnerabilityIds);
            autoResolvedVulnIds.addAll(findAutoResolvable(container.getId(), currentIds));

            int resolvedForContainer = autoResolve(container.getId(), scanId, currentIds);
            autoResolvedCount += resolvedForContainer;

            if (resolvedForContainer > 0) {
                log.info("Auto-resolved {} vulnerability instances for container {} in workload {}/{}",
                        resolvedForContainer, container.getName(), workload.getNamespace(), workload.getName());
            }
        }

        return new WorkloadIngestResult(vulnerabilityCount, criticalCount, autoResolvedCount,
                reopenedCount, resolved.rejectedCount(), autoResolvedVulnIds);
    }

    private ResolvedVulnerabilities resolveVulnerabilities(WorkloadData workloadData, Instant now) {
        Map<VulnerabilityKey, VulnerabilityData> distinct = new HashMap<>();
        int rejected = 0;

        for (ContainerData containerData : workloadData.getContainers()) {
            if (containerData.getVulnerabilities() == null) {
                continue;
            }
            for (VulnerabilityData vulnData : containerData.getVulnerabilities()) {
                String reason = VulnerabilityFieldGuard.rejectReason(vulnData);
                if (reason != null) {
                    log.warn("Skipping vulnerability {} for package {} in workload {}/{}: {}",
                            vulnData.getCveId(), vulnData.getPackageName(),
                            workloadData.getNamespace(), workloadData.getName(), reason);
                    rejected++;
                    continue;
                }
                distinct.putIfAbsent(VulnerabilityKey.of(vulnData), vulnData);
            }
        }

        Map<VulnerabilityKey, Vulnerability> byKey = new HashMap<>();
        distinct.keySet().stream()
                .sorted()
                .forEach(key -> byKey.put(key, upsertVulnerability(key, distinct.get(key), now)));

        return new ResolvedVulnerabilities(byKey, rejected);
    }

    private Vulnerability upsertVulnerability(VulnerabilityKey key, VulnerabilityData data, Instant now) {
        Vulnerability existing = vulnerabilityRepository.findByNaturalKey(
                key.cveId(), key.packageName(), key.packageVersion(), key.scannerType()).orElse(null);

        if (existing != null) {
            applyScannerUpdates(existing, data, now);
            return existing;
        }

        return vulnerabilityRepository.save(Vulnerability.builder()
                .cveId(data.getCveId())
                .packageName(data.getPackageName())
                .packageVersion(data.getPackageVersion())
                .severity(data.getSeverity())
                .title(data.getTitle())
                .description(data.getDescription())
                .primaryUrl(data.getPrimaryUrl())
                .publishedDate(data.getPublishedDate())
                .fixedVersion(data.getFixedVersion())
                .scannerType(data.getScannerType())
                .packageClass(truncate(data.getPackageClass(), PACKAGE_CLASS))
                .packageType(truncate(data.getPackageType(), PACKAGE_TYPE))
                .references(data.getReferences())
                .cvssV3Vector(truncate(data.getCvssV3Vector(), CVSS_V3_VECTOR))
                .cvssV3Score(data.getCvssV3Score())
                .firstDetected(data.getFirstDetected() != null ? data.getFirstDetected() : now)
                .lastSeen(now)
                .build());
    }

    private void applyScannerUpdates(Vulnerability vulnerability, VulnerabilityData data, Instant now) {
        updateIfChanged(data.getTitle(), vulnerability.getTitle(), vulnerability::setTitle);
        updateIfChanged(data.getDescription(), vulnerability.getDescription(), vulnerability::setDescription);
        updateIfChanged(data.getPrimaryUrl(), vulnerability.getPrimaryUrl(), vulnerability::setPrimaryUrl);
        updateIfChanged(data.getPublishedDate(), vulnerability.getPublishedDate(), vulnerability::setPublishedDate);
        updateIfChanged(data.getFixedVersion(), vulnerability.getFixedVersion(), vulnerability::setFixedVersion);
        updateIfChanged(truncate(data.getPackageClass(), PACKAGE_CLASS),
                vulnerability.getPackageClass(), vulnerability::setPackageClass);
        updateIfChanged(truncate(data.getPackageType(), PACKAGE_TYPE),
                vulnerability.getPackageType(), vulnerability::setPackageType);
        updateIfChanged(data.getReferences(), vulnerability.getReferences(), vulnerability::setReferences);
        updateIfChanged(truncate(data.getCvssV3Vector(), CVSS_V3_VECTOR),
                vulnerability.getCvssV3Vector(), vulnerability::setCvssV3Vector);
        updateIfChanged(data.getCvssV3Score(), vulnerability.getCvssV3Score(), vulnerability::setCvssV3Score);

        Instant lastSeen = vulnerability.getLastSeen();
        if (lastSeen == null || lastSeen.isBefore(now.minus(LAST_SEEN_REFRESH_INTERVAL))) {
            vulnerability.setLastSeen(now);
        }
    }

    private static <T> void updateIfChanged(T incoming, T current, Consumer<T> setter) {
        if (incoming != null && !incoming.equals(current)) {
            setter.accept(incoming);
        }
    }

    private boolean openOrReopenInstance(Scan scan, Container container, Vulnerability vulnerability) {
        Optional<VulnerabilityInstance> existing = vulnerabilityInstanceRepository
                .findByVulnerabilityIdAndContainerId(vulnerability.getId(), container.getId());

        if (existing.isEmpty()) {
            vulnerabilityInstanceRepository.save(VulnerabilityInstance.builder()
                    .vulnerability(vulnerability)
                    .container(container)
                    .scan(scan)
                    .status(VulnerabilityInstance.InstanceStatus.open)
                    .build());
            return false;
        }

        if (existing.get().getStatus() != VulnerabilityInstance.InstanceStatus.resolved) {
            return false;
        }

        int updated = vulnerabilityInstanceRepository.reopenResolvedForContainer(
                container.getId(), vulnerability.getId(), scan.getId());

        if (updated > 0) {
            log.debug("Reopened vulnerability instance: {} for container {} in workload {}/{}",
                    vulnerability.getCveId(), container.getName(),
                    container.getWorkload().getNamespace(), container.getWorkload().getName());
        }
        return updated > 0;
    }

    private List<Long> findAutoResolvable(long containerId, List<Long> currentVulnerabilityIds) {
        return currentVulnerabilityIds.isEmpty()
                ? vulnerabilityInstanceRepository.findAllOpenVulnerabilityIdsForContainer(containerId)
                : vulnerabilityInstanceRepository.findVulnerabilityIdsToAutoResolve(containerId, currentVulnerabilityIds);
    }

    private int autoResolve(long containerId, long scanId, List<Long> currentVulnerabilityIds) {
        String reason = String.format("Auto-resolved: Vulnerability no longer detected in scan %d", scanId);

        return currentVulnerabilityIds.isEmpty()
                ? vulnerabilityInstanceRepository.autoResolveAllForContainer(containerId, scanId, reason)
                : vulnerabilityInstanceRepository.autoResolveForContainer(
                        containerId, scanId, currentVulnerabilityIds, reason);
    }

    private Workload upsertWorkload(Scan scan, WorkloadData workloadData) {
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

        return workloadRepository.save(workload);
    }

    private Container upsertContainer(Workload workload, ContainerData containerData) {
        Container container = containerRepository
                .findByWorkloadIdAndName(workload.getId(), containerData.getName())
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
            updateIfChanged(containerData.getImageName(), container.getImageName(), container::setImageName);
            updateIfChanged(containerData.getImageTag(), container.getImageTag(), container::setImageTag);
            updateIfChanged(containerData.getImageCreated(), container.getImageCreated(), container::setImageCreated);
            updateIfChanged(containerData.getBaseImageCreated(), container.getBaseImageCreated(),
                    container::setBaseImageCreated);
            updateIfChanged(containerData.getDetectedEcosystems(), container.getDetectedEcosystems(),
                    container::setDetectedEcosystems);
            updateIfChanged(containerData.getOsFamily(), container.getOsFamily(), container::setOsFamily);
            updateIfChanged(containerData.getOsName(), container.getOsName(), container::setOsName);
            updateIfChanged(containerData.getRepoDigests(), container.getRepoDigests(), container::setRepoDigests);
        }

        return containerRepository.save(container);
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

    public record WorkloadIngestResult(int vulnerabilityCount, int criticalCount, int autoResolvedCount,
                                       int reopenedCount, int rejectedCount, Set<Long> autoResolvedVulnIds) {
    }

    private record ResolvedVulnerabilities(Map<VulnerabilityKey, Vulnerability> byKey, int rejectedCount) {
    }
}
