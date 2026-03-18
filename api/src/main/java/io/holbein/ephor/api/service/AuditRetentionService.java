package io.holbein.ephor.api.service;

import io.holbein.ephor.api.repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditRetentionService {

    private final AuditLogRepository auditLogRepository;

    @Value("${audit.retention-days:365}")
    private int retentionDays;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeExpiredEntries() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        long count = auditLogRepository.deleteByCreatedAtBefore(cutoff);
        if (count > 0) {
            log.info("Purged {} audit log entries older than {} days", count, retentionDays);
        }
    }
}
