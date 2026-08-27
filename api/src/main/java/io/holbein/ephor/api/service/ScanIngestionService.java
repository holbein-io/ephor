package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.ScanIngestRequest;
import io.holbein.ephor.api.dto.ScanIngestResponse;
import io.holbein.ephor.api.dto.WorkloadData;
import io.holbein.ephor.api.exception.ApiException;
import io.holbein.ephor.api.exception.ProblemType;
import io.holbein.ephor.api.model.enums.ScanStatus;
import io.holbein.ephor.api.service.WorkloadIngestionService.WorkloadIngestResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScanIngestionService {

    private final ScanRecordService scanRecordService;
    private final WorkloadIngestionService workloadIngestionService;
    private final RemediationService remediationService;
    private final RetryTemplate ingestRetryTemplate;

    public ScanIngestResponse ingestScan(ScanIngestRequest request) {
        log.info("Starting scan ingestion for namespace: {}, label: {}",
                request.getNamespace(), request.getScanLabel());

        long scanId = scanRecordService.create(request);
        log.debug("Created scan with ID: {}", scanId);

        boolean skipAutoResolve = request.getStatus() == ScanStatus.failed;
        if (skipAutoResolve) {
            log.warn("Scan status is 'failed' for namespace: {} -- skipping auto-resolve",
                    request.getNamespace());
        }

        int totalVulnerabilities = 0;
        int criticalVulns = 0;
        int totalAutoResolved = 0;
        int totalReopened = 0;
        int totalRejected = 0;
        int failedWorkloads = 0;
        Set<Long> allAutoResolvedVulnIds = new HashSet<>();

        for (WorkloadData workloadData : request.getWorkloads()) {
            try {
                WorkloadIngestResult result = ingestRetryTemplate.execute(context ->
                        workloadIngestionService.ingestWorkload(scanId, workloadData, skipAutoResolve));

                totalVulnerabilities += result.vulnerabilityCount();
                criticalVulns += result.criticalCount();
                totalAutoResolved += result.autoResolvedCount();
                totalReopened += result.reopenedCount();
                totalRejected += result.rejectedCount();
                allAutoResolvedVulnIds.addAll(result.autoResolvedVulnIds());
            } catch (Exception e) {
                failedWorkloads++;
                log.error("Workload ingestion failed for {}/{} in namespace {} (scan {}); "
                                + "continuing with the remaining workloads",
                        workloadData.getKind(), workloadData.getName(), request.getNamespace(), scanId, e);
            }
        }

        remediationService.autoCompleteRemediationsForVulnerabilities(allAutoResolvedVulnIds);

        int workloadCount = request.getWorkloads().size();
        if (failedWorkloads > 0) {
            scanRecordService.markFailed(scanId);

            if (failedWorkloads == workloadCount) {
                throw new ApiException(ProblemType.INTERNAL_ERROR,
                        "Scan ingestion failed for every workload in namespace " + request.getNamespace());
            }
        }

        log.info("Scan ingestion complete. Scan ID: {}, Workloads: {}, Failed workloads: {}, "
                        + "Vulnerabilities: {}, Rejected: {}, Critical: {}, Auto-resolved: {}, Reopened: {}",
                scanId, workloadCount, failedWorkloads, totalVulnerabilities, totalRejected,
                criticalVulns, totalAutoResolved, totalReopened);

        return ScanIngestResponse.builder()
                .scanId(scanId)
                .workloads(workloadCount)
                .failedWorkloads(failedWorkloads)
                .vulnerabilities(totalVulnerabilities)
                .rejectedVulnerabilities(totalRejected)
                .criticalVulns(criticalVulns)
                .autoResolved(totalAutoResolved)
                .reopened(totalReopened)
                .build();
    }
}
