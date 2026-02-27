package io.holbein.ephor.api.mapper.triage;

import io.holbein.ephor.api.dto.triage.preparation.AddPreparationRequest;
import io.holbein.ephor.api.dto.triage.preparation.PreparationResponse;
import io.holbein.ephor.api.dto.triage.shared.WorkloadSummary;
import io.holbein.ephor.api.entity.Container;
import io.holbein.ephor.api.entity.TriagePreparation;
import io.holbein.ephor.api.entity.TriageSession;
import io.holbein.ephor.api.entity.Vulnerability;
import io.holbein.ephor.api.entity.VulnerabilityInstance;
import io.holbein.ephor.api.entity.Workload;
import io.holbein.ephor.api.model.enums.PrepStatus;
import io.holbein.ephor.api.model.enums.PriorityFlag;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PreparationMapper {

    private PreparationMapper() {}

    public static TriagePreparation toEntity(AddPreparationRequest request, TriageSession session, Vulnerability vulnerability) {
        return TriagePreparation.builder()
                .triageSession(session)
                .vulnerability(vulnerability)
                .prepStatus(PrepStatus.pending)
                .prepNotes(request.prepNotes())
                .preliminaryDecision(request.preliminaryDecision())
                .priorityFlag(request.priorityFlag() != null ? request.priorityFlag() : PriorityFlag.medium)
                .prepBy(request.prepBy())
                .prepAt(Instant.now())
                .build();
    }

    public static PreparationResponse toResponse(TriagePreparation entity, boolean includeWorkloads) {
        Vulnerability vuln = entity.getVulnerability();
        List<WorkloadSummary> workloads = null;

        if (includeWorkloads && vuln.getInstances() != null) {
            // Group instances by workload, aggregate container images per workload
            Map<Long, List<VulnerabilityInstance>> byWorkload = new LinkedHashMap<>();
            for (VulnerabilityInstance vi : vuln.getInstances()) {
                Container c = vi.getContainer();
                if (c != null && c.getWorkload() != null) {
                    byWorkload.computeIfAbsent(c.getWorkload().getId(), k -> new java.util.ArrayList<>()).add(vi);
                }
            }
            workloads = byWorkload.values().stream()
                    .map(instances -> {
                        Workload w = instances.getFirst().getContainer().getWorkload();
                        String imageNames = instances.stream()
                                .map(vi -> vi.getContainer())
                                .map(c -> c.getImageName() + ":" + (c.getImageTag() != null ? c.getImageTag() : "latest"))
                                .distinct()
                                .collect(Collectors.joining(", "));
                        return new WorkloadSummary(
                                w.getId(),
                                w.getNamespace(),
                                w.getName(),
                                w.getKind() != null ? w.getKind().name() : null,
                                imageNames
                        );
                    })
                    .toList();
        }

        return new PreparationResponse(
                entity.getId(),
                entity.getTriageSession().getId(),
                vuln.getId(),
                entity.getPrepStatus(),
                entity.getPrepNotes(),
                entity.getPriorityFlag(),
                entity.getPreliminaryDecision(),
                entity.getPrepBy(),
                entity.getPrepAt(),
                vuln.getCveId(),
                vuln.getPackageName(),
                vuln.getPackageVersion(),
                vuln.getSeverity(),
                vuln.getTitle(),
                vuln.getDescription(),
                vuln.getPrimaryUrl(),
                vuln.getFixedVersion(),
                workloads
        );
    }
}
