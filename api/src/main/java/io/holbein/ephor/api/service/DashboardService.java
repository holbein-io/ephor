package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.dashboard.DashboardMetricsResponse;
import io.holbein.ephor.api.dto.dashboard.NamespaceComparisonResponse;
import io.holbein.ephor.api.dto.dashboard.VulnerabilityTrendResponse;
import io.holbein.ephor.api.entity.VulnerabilityInstance;
import io.holbein.ephor.api.model.enums.EscalationStatus;
import io.holbein.ephor.api.model.enums.SeverityLevel;
import io.holbein.ephor.api.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final VulnerabilityInstanceRepository instanceRepository;
    private final EscalationRepository escalationRepository;
    private final WorkloadRepository workloadRepository;

    public DashboardMetricsResponse getMetrics() {
        List<VulnerabilityInstance.InstanceStatus> activeStatuses = List.of(
                VulnerabilityInstance.InstanceStatus.open,
                VulnerabilityInstance.InstanceStatus.triaged
        );

        long totalVulnerabilities = instanceRepository.count();
        long totalActive = activeStatuses.stream()
                .mapToLong(instanceRepository::countByStatus)
                .sum();

        Map<String, Long> bySeverity = buildSeverityMap(activeStatuses);
        Map<String, Long> byStatus = buildStatusMap();

        long activeEscalations = escalationRepository.countByStatusIn(
                List.of(EscalationStatus.pending, EscalationStatus.acknowledged));

        return new DashboardMetricsResponse(
                totalVulnerabilities,
                totalActive,
                bySeverity,
                byStatus,
                activeEscalations
        );
    }

    public List<VulnerabilityTrendResponse> getTrends(int days) {
        List<Object[]> rows = instanceRepository.trendSnapshots(days);
        List<VulnerabilityTrendResponse> trends = new ArrayList<>();
        for (Object[] row : rows) {
            String date = ((String) row[0]).substring(0, 10);
            long total = ((Number) row[1]).longValue();
            long critical = ((Number) row[2]).longValue();
            long high = ((Number) row[3]).longValue();
            long medium = ((Number) row[4]).longValue();
            long low = ((Number) row[5]).longValue();
            trends.add(new VulnerabilityTrendResponse(date, total, critical, high, medium, low));
        }
        return trends;
    }

    public List<NamespaceComparisonResponse> getNamespaceComparison() {
        List<Object[]> rows = instanceRepository.namespaceComparison();
        List<NamespaceComparisonResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new NamespaceComparisonResponse(
                    (String) row[0],
                    ((Number) row[1]).longValue(),
                    ((Number) row[2]).longValue(),
                    ((Number) row[3]).longValue(),
                    ((Number) row[4]).longValue(),
                    ((Number) row[5]).longValue(),
                    ((Number) row[6]).longValue(),
                    ((Number) row[7]).longValue()
            ));
        }
        return result;
    }

    public List<String> getNamespaces() {
        return workloadRepository.findDistinctNamespaces();
    }

    private Map<String, Long> buildSeverityMap(List<VulnerabilityInstance.InstanceStatus> activeStatuses) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (SeverityLevel level : SeverityLevel.values()) {
            map.put(level.name(), 0L);
        }
        List<Object[]> rows = instanceRepository.countBySeverityGrouped(activeStatuses);
        for (Object[] row : rows) {
            SeverityLevel severity = (SeverityLevel) row[0];
            long count = (Long) row[1];
            map.put(severity.name(), count);
        }
        return map;
    }

    private Map<String, Long> buildStatusMap() {
        Map<String, Long> map = new LinkedHashMap<>();
        map.put("open", instanceRepository.countByStatus(VulnerabilityInstance.InstanceStatus.open));
        map.put("resolved", instanceRepository.countByStatus(VulnerabilityInstance.InstanceStatus.resolved));
        map.put("false_positive", instanceRepository.countByStatus(VulnerabilityInstance.InstanceStatus.false_positive));
        map.put("accepted_risk", instanceRepository.countByStatus(VulnerabilityInstance.InstanceStatus.accepted_risk));
        return map;
    }
}
