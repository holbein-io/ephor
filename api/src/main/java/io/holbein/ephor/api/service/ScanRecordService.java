package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.ScanIngestRequest;
import io.holbein.ephor.api.entity.Scan;
import io.holbein.ephor.api.model.enums.ScanStatus;
import io.holbein.ephor.api.repositories.ScanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
// Separate from ScanIngestionService so the scan row commits outside the per-workload
// transactions that orchestrator brackets.
public class ScanRecordService {

    private final ScanRepository scanRepository;

    @Transactional
    public long create(ScanIngestRequest request) {
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

        return scanRepository.save(scan).getId();
    }

    @Transactional
    public void markFailed(long scanId) {
        scanRepository.updateStatus(scanId, ScanStatus.failed);
    }
}
