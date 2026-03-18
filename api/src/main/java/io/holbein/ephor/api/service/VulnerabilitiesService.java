package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.vulnerability.*;
import io.holbein.ephor.api.entity.Comment;
import io.holbein.ephor.api.entity.Container;
import io.holbein.ephor.api.entity.Vulnerability;
import io.holbein.ephor.api.entity.VulnerabilityInstance;
import io.holbein.ephor.api.entity.VulnerabilityInstance.InstanceStatus;
import io.holbein.ephor.api.entity.Workload;
import io.holbein.ephor.api.exception.ResourceNotFoundException;
import io.holbein.ephor.api.repositories.CommentRepository;
import io.holbein.ephor.api.repositories.TriageDecisionRepository;
import io.holbein.ephor.api.repositories.VulnerabilityInstanceRepository;
import io.holbein.ephor.api.repositories.VulnerabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VulnerabilitiesService {

    private static final Set<InstanceStatus> RESOLVED_STATUSES = Set.of(
            InstanceStatus.resolved,
            InstanceStatus.false_positive,
            InstanceStatus.accepted_risk
    );

    private final VulnerabilityRepository vulnerabilityRepository;
    private final VulnerabilityInstanceRepository vulnerabilityInstanceRepository;
    private final CommentRepository commentRepository;
    private final TriageDecisionRepository triageDecisionRepository;

    public PaginatedResponse<VulnerabilityWithAffectedWorkloads> getAllVulnerabilities(VulnerabilityListQuery query) {
        return vulnerabilityRepository.findVulnerabilitiesWithFilters(query);
    }

    public Vulnerability getVulnerabilityById(long id) {
        return vulnerabilityRepository.getVulnerabilityById(id);
    }

    @Transactional(readOnly = true)
    public VulnerabilityDetailResponse getVulnerabilityDetail(long id) {
        Vulnerability v = vulnerabilityRepository.findByIdWithWorkloads(id)
                .orElseThrow(() -> ResourceNotFoundException.vulnerability(id));

        // Group instances by workload, collecting container images and worst status per workload
        Map<Long, List<VulnerabilityInstance>> instancesByWorkload = new LinkedHashMap<>();
        for (VulnerabilityInstance vi : v.getInstances()) {
            Container c = vi.getContainer();
            if (c != null && c.getWorkload() != null) {
                instancesByWorkload.computeIfAbsent(c.getWorkload().getId(), k -> new ArrayList<>()).add(vi);
            }
        }

        List<VulnerabilityDetailResponse.WorkloadInfo> workloads = instancesByWorkload.values().stream()
                .map(instances -> {
                    Workload w = instances.getFirst().getContainer().getWorkload();
                    String imageNames = instances.stream()
                            .map(vi -> vi.getContainer())
                            .map(c -> c.getImageName() + ":" + (c.getImageTag() != null ? c.getImageTag() : "latest"))
                            .distinct()
                            .collect(java.util.stream.Collectors.joining(", "));
                    String status = instances.stream()
                            .map(vi -> vi.getStatus())
                            .map(s -> s != null ? s.name() : null)
                            .distinct()
                            .reduce((a, b) -> "mixed")
                            .orElse(null);
                    return new VulnerabilityDetailResponse.WorkloadInfo(
                            w.getId(),
                            w.getNamespace(),
                            w.getName(),
                            w.getKind() != null ? w.getKind().name() : null,
                            imageNames,
                            status
                    );
                })
                .toList();

        // Compute aggregated status from all instances
        Set<String> allStatuses = v.getInstances().stream()
                .map(vi -> vi.getStatus() != null ? vi.getStatus().name() : "open")
                .collect(java.util.stream.Collectors.toSet());
        String status;
        if (allStatuses.size() == 1) {
            status = allStatuses.iterator().next();
        } else if (allStatuses.size() > 1) {
            status = "mixed";
        } else {
            status = "open";
        }

        return new VulnerabilityDetailResponse(
                v.getId(),
                v.getCveId(),
                v.getPackageName(),
                v.getPackageVersion(),
                v.getSeverity(),
                v.getTitle(),
                v.getDescription(),
                v.getPrimaryUrl(),
                v.getPublishedDate(),
                v.getFixedVersion(),
                v.getScannerType(),
                v.getFirstDetected(),
                v.getLastSeen(),
                workloads.size(),
                status,
                workloads
        );
    }

    @Transactional
    public int updateVulnerabilityStatus(long id, InstanceStatus status, boolean applyToAll) {
        // applyToAll: update ALL instances regardless of current status
        // !applyToAll with mixed status: only update open instances
        // !applyToAll with uniform status: update all instances (allows resolved -> accepted_risk etc.)
        if (RESOLVED_STATUSES.contains(status)) {
            if (applyToAll) {
                return vulnerabilityInstanceRepository.updateStatusWithResolvedAt(id, status);
            } else {
                // Try open instances first, if none, apply to all
                int updated = vulnerabilityInstanceRepository.updateStatusWithResolvedAtByCurrentStatus(
                        id, InstanceStatus.open, status);
                if (updated == 0) {
                    updated = vulnerabilityInstanceRepository.updateStatusWithResolvedAt(id, status);
                }
                return updated;
            }
        } else {
            return vulnerabilityInstanceRepository.updateStatusByVulnerabilityId(id, status);
        }
    }

    public List<Comment> getCommentsByVulnerabilityId(long vulnerabilityId) {
        return commentRepository.findByVulnerabilityIdOrderByCreatedAtDesc(vulnerabilityId);
    }

    @Transactional
    public Comment addComment(long vulnerabilityId, CommentRequest request, String defaultAuthor) {
        Vulnerability vulnerability = vulnerabilityRepository.getVulnerabilityById(vulnerabilityId);
        if (vulnerability == null) {
            return null;
        }

        String author = request.getAuthor() != null ? request.getAuthor() : defaultAuthor;

        Comment comment = Comment.builder()
                .vulnerability(vulnerability)
                .createdBy(author)
                .body(request.getComment())
                .commentType(request.getCommentType())
                .build();

        return commentRepository.save(comment);
    }

    @Transactional
    public boolean deleteComment(long commentId, long vulnerabilityId) {
        Optional<Comment> comment = commentRepository.findByIdAndVulnerabilityId(commentId, vulnerabilityId);
        if (comment.isEmpty()) {
            return false;
        }
        commentRepository.delete(comment.get());
        return true;
    }

    public TriageInfoResponse getTriageInfo(long vulnerabilityId) {
        TriageDecisionRepository.TriageInfo info =
                triageDecisionRepository.findLatestTriageInfoByVulnerabilityId(vulnerabilityId);
        if (info == null) {
            return null;
        }
        return new TriageInfoResponse(
                info.getDecisionId(),
                info.getTriageStatus(),
                info.getTriageNotes(),
                info.getAssignedTo(),
                info.getTargetDate(),
                info.getDecisionDate(),
                info.getSessionDate(),
                info.getAttendees(),
                info.getRemediationId(),
                info.getRemediationStatus(),
                info.getRemediationPriority(),
                info.getRemediationCompleted()
        );
    }

    @Transactional
    public AutoResolveResponse autoResolveVulnerabilities(int gracePeriodDays, boolean dryRun) {
        Instant cutoffDate = Instant.now().minus(gracePeriodDays, ChronoUnit.DAYS);

        List<VulnerabilityInstance> candidates = vulnerabilityInstanceRepository.findAutoResolveCandidates(cutoffDate);

        // Group by vulnerability to avoid duplicates
        Map<Long, VulnerabilityInstance> uniqueVulnerabilities = new LinkedHashMap<>();
        for (VulnerabilityInstance vi : candidates) {
            uniqueVulnerabilities.putIfAbsent(vi.getVulnerability().getId(), vi);
        }

        List<AutoResolveResponse.Candidate> candidateList = uniqueVulnerabilities.values().stream()
                .map(vi -> AutoResolveResponse.Candidate.builder()
                        .id(vi.getVulnerability().getId())
                        .cveId(vi.getVulnerability().getCveId())
                        .packageName(vi.getVulnerability().getPackageName())
                        .lastSeen(vi.getVulnerability().getLastSeen())
                        .build())
                .toList();

        if (dryRun) {
            return AutoResolveResponse.builder()
                    .resolved(0)
                    .candidates(candidateList)
                    .build();
        }

        int resolved = 0;
        for (VulnerabilityInstance vi : uniqueVulnerabilities.values()) {
            Vulnerability vuln = vi.getVulnerability();
            String reason = String.format("Auto-resolved: Not detected for %d days since %s",
                    gracePeriodDays, vuln.getLastSeen());

            int updated = vulnerabilityInstanceRepository.autoResolveByVulnerabilityId(vuln.getId(), reason);
            if (updated > 0) {
                resolved++;
                log.info("Auto-resolved vulnerability {} (ID: {})", vuln.getCveId(), vuln.getId());
            }
        }

        return AutoResolveResponse.builder()
                .resolved(resolved)
                .candidates(candidateList)
                .build();
    }

    @Transactional
    public boolean reopenAutoResolvedVulnerability(long vulnerabilityId) {
        int updated = vulnerabilityInstanceRepository.reopenAutoResolved(vulnerabilityId);
        if (updated > 0) {
            log.info("Reopened auto-resolved vulnerability ID: {}", vulnerabilityId);
            return true;
        }
        return false;
    }
}
