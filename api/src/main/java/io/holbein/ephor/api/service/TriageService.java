package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.triage.bulkplan.*;
import io.holbein.ephor.api.dto.triage.decision.CreateDecisionRequest;
import io.holbein.ephor.api.dto.triage.decision.DecisionResponse;
import io.holbein.ephor.api.dto.triage.preparation.AddPreparationRequest;
import io.holbein.ephor.api.dto.triage.preparation.PreparationResponse;
import io.holbein.ephor.api.dto.triage.preparation.UpdatePreparationRequest;
import io.holbein.ephor.api.dto.triage.report.TriageReportResponse;
import io.holbein.ephor.api.dto.triage.report.VulnerabilityForTriage;
import io.holbein.ephor.api.dto.triage.shared.WorkloadSummary;
import io.holbein.ephor.api.dto.triage.session.ChangeSessionStatusRequest;
import io.holbein.ephor.api.dto.triage.session.CreateSessionRequest;
import io.holbein.ephor.api.dto.triage.session.SessionResponse;
import io.holbein.ephor.api.dto.triage.session.UpdateSessionRequest;
import io.holbein.ephor.api.dto.triage.shared.DeleteResponse;
import io.holbein.ephor.api.entity.*;
import io.holbein.ephor.api.mapper.triage.BulkPlanMapper;
import io.holbein.ephor.api.mapper.triage.DecisionMapper;
import io.holbein.ephor.api.mapper.triage.PreparationMapper;
import io.holbein.ephor.api.mapper.triage.SessionMapper;
import io.holbein.ephor.api.model.enums.*;
import io.holbein.ephor.api.repositories.*;
import io.holbein.ephor.api.service.triage.BulkPlanFilterMatcher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TriageService {

    @PersistenceContext
    private EntityManager entityManager;

    private final TriageSessionRepository triageSessionRepository;
    private final TriagePreparationsRepository triagePreparationsRepository;
    private final TriageBulkPlanRepository triageBulkPlanRepository;
    private final TriageDecisionRepository triageDecisionRepository;
    private final TriageBulkOperationRepository triageBulkOperationRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final VulnerabilityInstanceRepository vulnerabilityInstanceRepository;

    @Transactional(readOnly = true)
    public List<SessionResponse> getTriageSessions(SessionStatus status) {
        List<TriageSession> sessions = status != null
                ? triageSessionRepository.findByStatusOrderBySessionDateDesc(status)
                : triageSessionRepository.findAllByOrderBySessionDateDesc();
        return sessions.stream()
                .map(SessionMapper::toResponse)
                .toList();
    }

    @Transactional
    public SessionResponse createTriageSession(CreateSessionRequest request) {
        TriageSession session = SessionMapper.toEntity(request);
        triageSessionRepository.save(session);
        return SessionMapper.toResponse(session);
    }

    @Transactional(readOnly = true)
    public SessionResponse getTriageSession(long id) {
        return triageSessionRepository.getTriageSessionsById(id)
                .map(SessionMapper::toResponse)
                .orElse(null);
    }

    @Transactional
    public SessionResponse updateTriageSession(long id, UpdateSessionRequest request) {
        TriageSession session = triageSessionRepository.getReferenceById(id);
        if (request.prepNotes() != null) {
            session.setPrepNotes(request.prepNotes());
        }
        if (request.notes() != null) {
            session.setNotes(request.notes());
        }
        if (request.attendees() != null) {
            session.setAttendees(request.attendees());
        }
        triageSessionRepository.save(session);
        return SessionMapper.toResponse(session);
    }

    @Transactional
    public DeleteResponse deleteTriageSession(long id) {
        return triageSessionRepository.getTriageSessionsById(id)
                .map(session -> {
                    triageSessionRepository.deleteById(id);
                    return new DeleteResponse(true,
                            String.format("Session with id %s has been deleted", id));
                })
                .orElse(new DeleteResponse(false,
                        String.format("Session with id %s not found", id)));
    }

    @Transactional
    public SessionResponse changeSessionStatus(long id, ChangeSessionStatusRequest request) {
        TriageSession session = triageSessionRepository.getReferenceById(id);
        SessionStatus targetStatus = request.status();
        Instant now = Instant.now();

        switch (targetStatus) {
            case ACTIVE -> {
                session.setPrepCompletedAt(now);
                session.setSessionStartedAt(now);
            }
            case COMPLETED -> {
                session.setCompletedAt(now);
                if (session.getSessionStartedAt() != null) {
                    long durationMinutes = Duration.between(session.getSessionStartedAt(), now).toMinutes();
                    session.setSessionDurationMinutes((int) durationMinutes);
                }
                if (session.getCreatedAt() != null && session.getPrepCompletedAt() != null) {
                    long prepMinutes = Duration.between(session.getCreatedAt(), session.getPrepCompletedAt()).toMinutes();
                    session.setPrepDurationMinutes((int) prepMinutes);
                }
            }
            case CANCELLED -> {
                session.setCompletedAt(now);
                if (request.notes() != null) {
                    session.setNotes(request.notes());
                }
            }
            default -> {}
        }

        session.setStatus(targetStatus);
        triageSessionRepository.save(session);
        return SessionMapper.toResponse(session);
    }

    @Transactional(readOnly = true)
    public List<PreparationResponse> getSessionPreparations(long sessionId, boolean includeWorkloads) {
        return triageSessionRepository.getTriageSessionsById(sessionId)
                .map(session -> session.getPreparations().stream()
                        .map(prep -> PreparationMapper.toResponse(prep, includeWorkloads))
                        .toList())
                .orElse(List.of());
    }

    @Transactional
    public PreparationResponse createPreparation(AddPreparationRequest request) {
        TriageSession session = triageSessionRepository.getReferenceById(request.sessionId());
        Vulnerability vulnerability = vulnerabilityRepository.getVulnerabilityById(request.vulnerabilityId());

        TriagePreparation preparation = PreparationMapper.toEntity(request, session, vulnerability);
        triagePreparationsRepository.save(preparation);

        return PreparationMapper.toResponse(preparation, false);
    }

    @Transactional
    public PreparationResponse updatePreparation(long preparationId, UpdatePreparationRequest request) {
        TriagePreparation preparation = triagePreparationsRepository.getReferenceById(preparationId);
        if (request.prepNotes() != null) {
            preparation.setPrepNotes(request.prepNotes());
        }
        if (request.preliminaryDecision() != null) {
            preparation.setPreliminaryDecision(request.preliminaryDecision());
        }
        if (request.priorityFlag() != null) {
            preparation.setPriorityFlag(request.priorityFlag());
        }
        if (request.prepStatus() != null) {
            preparation.setPrepStatus(request.prepStatus());
        }
        triagePreparationsRepository.save(preparation);

        return PreparationMapper.toResponse(preparation, true);
    }

    @Transactional
    public DeleteResponse deletePreparation(long preparationId) {
        try {
            TriagePreparation preparation = triagePreparationsRepository.getReferenceById(preparationId);
            triagePreparationsRepository.delete(preparation);
            return new DeleteResponse(true,
                    String.format("Successfully deleted preparation with id %s", preparationId));
        } catch (Exception ex) {
            log.error("Failed to delete preparation with id {}", preparationId, ex);
            return new DeleteResponse(false,
                    String.format("Failed to delete preparation with id %s", preparationId));
        }
    }

    @Transactional(readOnly = true)
    public TriageReportResponse getTriageReport(int days, String namespace,
                                                List<String> severity, boolean excludeDecided) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        // Lookback period
        conditions.add("v.first_detected >= NOW() - CAST(:days || ' days' AS INTERVAL)");
        params.put("days", String.valueOf(days));

        // Only open instances
        conditions.add("vi.status = 'open'");

        if (namespace != null && !namespace.isBlank()) {
            conditions.add("w.namespace = :namespace");
            params.put("namespace", namespace);
        }

        if (severity != null && !severity.isEmpty()) {
            conditions.add("v.severity IN (:severities)");
            params.put("severities", severity);
        }

        if (excludeDecided) {
            conditions.add("NOT EXISTS (SELECT 1 FROM triage_decisions td " +
                    "JOIN triage_sessions ts ON td.session_id = ts.id " +
                    "WHERE td.vulnerability_id = v.id " +
                    "AND ts.status IN ('ACTIVE', 'COMPLETED'))");
        }

        String whereClause = "WHERE " + String.join(" AND ", conditions);

        String sql = String.format("""
                SELECT
                    v.id, v.cve_id, v.package_name, v.package_version, v.severity,
                    v.title, v.description, v.primary_url, v.fixed_version,
                    v.first_detected, v.last_seen,
                    COUNT(DISTINCT (w.namespace, w.name, w.kind)) AS affected_workloads
                FROM vulnerabilities v
                JOIN vulnerability_instances vi ON v.id = vi.vulnerability_id
                JOIN containers c ON vi.container_id = c.id
                JOIN workloads w ON c.workload_id = w.id
                %s
                GROUP BY v.id
                ORDER BY
                    CASE v.severity
                        WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' THEN 1
                        WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 ELSE 4
                    END,
                    v.first_detected DESC
                """, whereClause);

        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<Long> vulnIds = rows.stream()
                .map(r -> ((Number) r[0]).longValue())
                .toList();

        // Batch-load workload details for all matching vulnerabilities
        Map<Long, List<WorkloadSummary>> workloadsByVuln = loadWorkloadSummaries(vulnIds, namespace);

        List<VulnerabilityForTriage> vulnerabilities = rows.stream()
                .map(row -> {
                    long vulnId = ((Number) row[0]).longValue();
                    return new VulnerabilityForTriage(
                            vulnId,
                            (String) row[1],
                            (String) row[2],
                            (String) row[3],
                            row[4] != null ? SeverityLevel.valueOf((String) row[4]) : null,
                            (String) row[5],
                            (String) row[6],
                            (String) row[7],
                            (String) row[8],
                            row[9] != null ? ((Timestamp) row[9]).toInstant() : null,
                            row[10] != null ? ((Timestamp) row[10]).toInstant() : null,
                            ((Number) row[11]).intValue(),
                            workloadsByVuln.getOrDefault(vulnId, List.of())
                    );
                })
                .toList();

        return new TriageReportResponse(days, namespace, vulnerabilities.size(), vulnerabilities);
    }

    private Map<Long, List<WorkloadSummary>> loadWorkloadSummaries(List<Long> vulnIds, String namespace) {
        if (vulnIds.isEmpty()) return Map.of();

        String sql = """
                SELECT vi.vulnerability_id, w.id, w.namespace, w.name, w.kind,
                       STRING_AGG(DISTINCT CONCAT(c.image_name, ':', COALESCE(c.image_tag, 'latest')), ', ') as image_names
                FROM vulnerability_instances vi
                JOIN containers c ON vi.container_id = c.id
                JOIN workloads w ON c.workload_id = w.id
                WHERE vi.vulnerability_id IN (:vulnIds)
                AND vi.status = 'open'
                """ + (namespace != null && !namespace.isBlank()
                ? "AND w.namespace = :namespace " : "") +
                "GROUP BY vi.vulnerability_id, w.id, w.namespace, w.name, w.kind " +
                "ORDER BY vi.vulnerability_id, w.namespace, w.name";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("vulnIds", vulnIds);
        if (namespace != null && !namespace.isBlank()) {
            query.setParameter("namespace", namespace);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        Map<Long, List<WorkloadSummary>> result = new HashMap<>();
        for (Object[] row : rows) {
            long vulnId = ((Number) row[0]).longValue();
            WorkloadSummary ws = new WorkloadSummary(
                    ((Number) row[1]).longValue(),
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    (String) row[5]
            );
            result.computeIfAbsent(vulnId, k -> new ArrayList<>()).add(ws);
        }
        return result;
    }

    @Transactional
    public DecisionResponse createDecision(CreateDecisionRequest request) {
        TriageSession session = triageSessionRepository.getReferenceById(request.sessionId());

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new IllegalStateException("Session must be in ACTIVE status to create decisions");
        }

        Vulnerability vulnerability = vulnerabilityRepository.getVulnerabilityById(request.vulnerabilityId());

        boolean preparedInSession = session.getPreparations().stream()
                .anyMatch(prep -> prep.getVulnerability().getId() == request.vulnerabilityId());

        if (!preparedInSession) {
            throw new IllegalStateException("Vulnerability must be in session preparations before a decision can be made");
        }

        boolean alreadyDecided = triageDecisionRepository
                .existsByTriageSessionIdAndVulnerabilityId(session.getId(), request.vulnerabilityId());
        if (alreadyDecided) {
            throw new IllegalStateException("Decision already exists for this vulnerability in this session");
        }

        TriageDecision decision = DecisionMapper.toEntity(request, session, vulnerability);
        triageDecisionRepository.save(decision);

        if (request.status() == DecisionStatus.needs_remediation) {
            Remediation remediation = Remediation.builder()
                    .vulnerability(vulnerability)
                    .triageDecision(decision)
                    .assignedTo(request.assignedTo())
                    .targetDate(request.targetDate())
                    .priority(request.priority())
                    .notes(request.notes())
                    .build();
            decision.setRemediation(remediation);
            triageDecisionRepository.save(decision);
        }

        // Update open vulnerability instances to triaged status
        vulnerabilityInstanceRepository.updateStatusByVulnerabilityIdAndCurrentStatus(
                request.vulnerabilityId(),
                VulnerabilityInstance.InstanceStatus.open,
                VulnerabilityInstance.InstanceStatus.triaged);

        return DecisionMapper.toResponse(decision);
    }

    @Transactional(readOnly = true)
    public List<DecisionResponse> getSessionDecisions(long sessionId) {
        return triageSessionRepository.getTriageSessionsById(sessionId)
                .map(session -> session.getDecisions().stream()
                        .map(DecisionMapper::toResponse)
                        .toList())
                .orElse(List.of());
    }

    @Transactional
    public BulkPlanResponse createBulkPlan(CreateBulkPlanRequest request) {
        TriageSession session = triageSessionRepository.getReferenceById(request.sessionId());
        TriageBulkPlan bulkPlan = BulkPlanMapper.toEntity(request, session);
        bulkPlan.setCreatedDuringPrep(session.getStatus() == SessionStatus.PREPARING);

        // Calculate estimated count and matching vulnerabilities preview
        BulkPlanFilters filters = request.filters();
        Set<Long> decidedVulnIds = new HashSet<>(
                triageDecisionRepository.findDecidedVulnerabilityIdsBySessionId(session.getId()));

        List<TriagePreparation> matched = resolveFilters(session, filters, decidedVulnIds);
        bulkPlan.setEstimatedCount(matched.size());

        triageBulkPlanRepository.save(bulkPlan);

        List<MatchingVulnerability> preview = buildPreview(matched, 50);
        return BulkPlanMapper.toResponse(bulkPlan, preview);
    }

    @Transactional(readOnly = true)
    public List<BulkPlanResponse> getSessionBulkPlans(long sessionId, BulkPlanStatus status) {
        List<TriageBulkPlan> plans = status != null
                ? triageBulkPlanRepository.findBySessionId(sessionId, status)
                : triageBulkPlanRepository.findByTriageSessionId(sessionId);

        return plans.stream()
                .map(BulkPlanMapper::toResponse)
                .toList();
    }

    @Transactional
    public ExecuteBulkPlanResponse executeBulkPlan(long planId, ExecuteBulkPlanRequest request) {
        TriageBulkPlan bulkPlan = triageBulkPlanRepository.getReferenceById(planId);
        TriageSession session = bulkPlan.getTriageSession();

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new IllegalStateException("Session must be in ACTIVE status to execute bulk plans");
        }

        if (bulkPlan.getStatus() != BulkPlanStatus.planned && bulkPlan.getStatus() != BulkPlanStatus.ready) {
            throw new IllegalStateException("Bulk plan must be in planned or ready status to execute");
        }

        BulkPlanFilters filters = BulkPlanMapper.parseFilters(bulkPlan.getFilters());
        BulkPlanMetadata metadata = BulkPlanMapper.parseMetadata(bulkPlan.getMetadata());
        Set<Long> decidedVulnIds = new HashSet<>(
                triageDecisionRepository.findDecidedVulnerabilityIdsBySessionId(session.getId()));

        List<TriagePreparation> allPreps = session.getPreparations();
        List<CreatedDecision> createdDecisions = new ArrayList<>();
        List<Integer> affectedVulnIds = new ArrayList<>();
        int skippedCount = 0;

        for (TriagePreparation prep : allPreps) {
            long vulnId = prep.getVulnerability().getId();

            if (decidedVulnIds.contains(vulnId)) {
                skippedCount++;
                continue;
            }

            if (!BulkPlanFilterMatcher.matches(filters, prep)) {
                continue;
            }

            Vulnerability vuln = prep.getVulnerability();
            DecisionStatus decisionStatus = mapActionToDecisionStatus(bulkPlan.getAction());

            TriageDecision decision = TriageDecision.builder()
                    .triageSession(session)
                    .vulnerability(vuln)
                    .decision(decisionStatus)
                    .decidedBy(request.executedBy())
                    .notes(metadata != null ? metadata.reason() : null)
                    .assignedTo(metadata != null ? metadata.assignedTo() : null)
                    .targetDate(metadata != null ? metadata.targetDate() : null)
                    .build();

            if (bulkPlan.getAction() == BulkAction.needs_remediation) {
                Remediation remediation = Remediation.builder()
                        .vulnerability(vuln)
                        .triageDecision(decision)
                        .assignedTo(metadata != null ? metadata.assignedTo() : null)
                        .targetDate(metadata != null ? metadata.targetDate() : null)
                        .priority(metadata != null ? metadata.priority() : null)
                        .notes(metadata != null ? metadata.reason() : null)
                        .build();
                decision.setRemediation(remediation);
            }

            triageDecisionRepository.save(decision);

            // Update open vulnerability instances to triaged status
            vulnerabilityInstanceRepository.updateStatusByVulnerabilityIdAndCurrentStatus(
                    vulnId,
                    VulnerabilityInstance.InstanceStatus.open,
                    VulnerabilityInstance.InstanceStatus.triaged);

            decidedVulnIds.add(vulnId);
            affectedVulnIds.add((int) vulnId);

            createdDecisions.add(new CreatedDecision(
                    decision.getId(),
                    vulnId,
                    vuln.getCveId(),
                    vuln.getSeverity(),
                    vuln.getPackageName(),
                    decisionStatus
            ));
        }

        // Update bulk plan
        Instant now = Instant.now();
        bulkPlan.setStatus(BulkPlanStatus.executed);
        bulkPlan.setActualCount(createdDecisions.size());
        bulkPlan.setExecutedBy(request.executedBy());
        bulkPlan.setExecutedAt(now);
        triageBulkPlanRepository.save(bulkPlan);

        // Record audit trail
        TriageBulkOperation operation = TriageBulkOperation.builder()
                .triageSession(session)
                .bulkPlan(bulkPlan)
                .vulnerabilityIds(affectedVulnIds)
                .operationType(bulkPlan.getAction().name())
                .metadata(bulkPlan.getMetadata())
                .executedBy(request.executedBy())
                .build();
        triageBulkOperationRepository.save(operation);

        return new ExecuteBulkPlanResponse(
                bulkPlan.getId(),
                BulkPlanStatus.executed,
                now,
                request.executedBy(),
                bulkPlan.getEstimatedCount(),
                createdDecisions.size(),
                skippedCount,
                createdDecisions
        );
    }

    @Transactional
    public BulkPlanResponse cancelBulkPlan(long planId) {
        TriageBulkPlan bulkPlan = triageBulkPlanRepository.getReferenceById(planId);

        if (bulkPlan.getStatus() != BulkPlanStatus.planned && bulkPlan.getStatus() != BulkPlanStatus.ready) {
            throw new IllegalStateException("Only plans in planned or ready status can be cancelled");
        }

        bulkPlan.setStatus(BulkPlanStatus.cancelled);
        triageBulkPlanRepository.save(bulkPlan);
        return BulkPlanMapper.toResponse(bulkPlan);
    }

    @Transactional(readOnly = true)
    public BulkPlanResponse previewBulkPlan(long planId) {
        TriageBulkPlan bulkPlan = triageBulkPlanRepository.getReferenceById(planId);
        TriageSession session = bulkPlan.getTriageSession();

        BulkPlanFilters filters = BulkPlanMapper.parseFilters(bulkPlan.getFilters());
        Set<Long> decidedVulnIds = new HashSet<>(
                triageDecisionRepository.findDecidedVulnerabilityIdsBySessionId(session.getId()));

        List<TriagePreparation> matched = resolveFilters(session, filters, decidedVulnIds);

        // Update estimated count to reflect current state
        bulkPlan.setEstimatedCount(matched.size());

        List<MatchingVulnerability> preview = buildPreview(matched, 50);
        return BulkPlanMapper.toResponse(bulkPlan, preview);
    }

    @Transactional
    public DeleteResponse deleteBulkPlan(long planId) {
        TriageBulkPlan bulkPlan = triageBulkPlanRepository.getReferenceById(planId);

        if (bulkPlan.getStatus() == BulkPlanStatus.executed) {
            return new DeleteResponse(false, "Cannot delete an executed bulk plan");
        }

        triageBulkPlanRepository.delete(bulkPlan);
        return new DeleteResponse(true,
                String.format("Bulk plan with id %s has been deleted", planId));
    }

    private List<TriagePreparation> resolveFilters(TriageSession session,
                                                    BulkPlanFilters filters,
                                                    Set<Long> decidedVulnIds) {
        return session.getPreparations().stream()
                .filter(prep -> !decidedVulnIds.contains(prep.getVulnerability().getId()))
                .filter(prep -> BulkPlanFilterMatcher.matches(filters, prep))
                .toList();
    }

    private List<MatchingVulnerability> buildPreview(List<TriagePreparation> matched, int limit) {
        return matched.stream()
                .limit(limit)
                .map(prep -> {
                    Vulnerability vuln = prep.getVulnerability();
                    return new MatchingVulnerability(
                            vuln.getId(),
                            vuln.getCveId(),
                            vuln.getSeverity(),
                            vuln.getPackageName(),
                            prep.getPriorityFlag(),
                            prep.getPrepStatus()
                    );
                })
                .toList();
    }

    private DecisionStatus mapActionToDecisionStatus(BulkAction action) {
        return switch (action) {
            case accept_risk -> DecisionStatus.accepted_risk;
            case false_positive -> DecisionStatus.false_positive;
            case needs_remediation -> DecisionStatus.needs_remediation;
        };
    }
}
