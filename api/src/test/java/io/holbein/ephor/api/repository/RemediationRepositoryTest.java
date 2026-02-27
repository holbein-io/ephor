package io.holbein.ephor.api.repository;

import io.holbein.ephor.api.entity.Remediation;
import io.holbein.ephor.api.entity.Vulnerability;
import io.holbein.ephor.api.model.enums.CompletionMethod;
import io.holbein.ephor.api.model.enums.RemediationPriority;
import io.holbein.ephor.api.model.enums.RemediationStatus;
import io.holbein.ephor.api.model.enums.SeverityLevel;
import io.holbein.ephor.api.repositories.RemediationRepository;
import io.holbein.ephor.api.repositories.VulnerabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RemediationRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private RemediationRepository remediationRepository;

    @Autowired
    private VulnerabilityRepository vulnerabilityRepository;

    private Vulnerability vulnerability;

    @BeforeEach
    void setUp() {
        remediationRepository.deleteAll();
        vulnerabilityRepository.deleteAll();

        vulnerability = vulnerabilityRepository.save(Vulnerability.builder()
                .cveId("CVE-2025-0001")
                .packageName("openssl")
                .packageVersion("3.0.1")
                .severity(SeverityLevel.CRITICAL)
                .scannerType("trivy")
                .build());
    }

    @Test
    void findOverdue_returnsOnlyActiveWithPastTargetDate() {
        remediationRepository.save(Remediation.builder()
                .vulnerability(vulnerability)
                .status(RemediationStatus.planned)
                .priority(RemediationPriority.high)
                .targetDate(LocalDate.now().minusDays(5))
                .build());

        remediationRepository.save(Remediation.builder()
                .vulnerability(vulnerability)
                .status(RemediationStatus.in_progress)
                .priority(RemediationPriority.medium)
                .targetDate(LocalDate.now().minusDays(1))
                .build());

        remediationRepository.save(Remediation.builder()
                .vulnerability(vulnerability)
                .status(RemediationStatus.planned)
                .priority(RemediationPriority.low)
                .targetDate(LocalDate.now().plusDays(10))
                .build());

        remediationRepository.save(Remediation.builder()
                .vulnerability(vulnerability)
                .status(RemediationStatus.completed)
                .priority(RemediationPriority.high)
                .targetDate(LocalDate.now().minusDays(3))
                .completedAt(Instant.now())
                .completionMethod(CompletionMethod.manual)
                .completedBy("user1")
                .build());

        var activeStatuses = List.of(RemediationStatus.planned, RemediationStatus.in_progress);
        List<Remediation> overdue = remediationRepository.findOverdue(activeStatuses, LocalDate.now());

        assertThat(overdue).hasSize(2);
        assertThat(overdue).extracting(Remediation::getStatus)
                .containsExactlyInAnyOrder(RemediationStatus.planned, RemediationStatus.in_progress);
    }

    @Test
    void findOverdueFiltered_filtersByPriorityAndAssignee() {
        remediationRepository.save(Remediation.builder()
                .vulnerability(vulnerability)
                .status(RemediationStatus.planned)
                .priority(RemediationPriority.high)
                .assignedTo("alice")
                .targetDate(LocalDate.now().minusDays(5))
                .build());

        remediationRepository.save(Remediation.builder()
                .vulnerability(vulnerability)
                .status(RemediationStatus.planned)
                .priority(RemediationPriority.low)
                .assignedTo("bob")
                .targetDate(LocalDate.now().minusDays(5))
                .build());

        var activeStatuses = List.of(RemediationStatus.planned, RemediationStatus.in_progress);

        List<Remediation> highPriority = remediationRepository.findOverdueFiltered(
                activeStatuses, LocalDate.now(), RemediationPriority.high, null);
        assertThat(highPriority).hasSize(1);
        assertThat(highPriority.get(0).getAssignedTo()).isEqualTo("alice");

        List<Remediation> bobItems = remediationRepository.findOverdueFiltered(
                activeStatuses, LocalDate.now(), null, "bob");
        assertThat(bobItems).hasSize(1);
        assertThat(bobItems.get(0).getPriority()).isEqualTo(RemediationPriority.low);

        List<Remediation> all = remediationRepository.findOverdueFiltered(
                activeStatuses, LocalDate.now(), null, null);
        assertThat(all).hasSize(2);
    }

    @Test
    void countByStatus_returnsCorrectCounts() {
        remediationRepository.save(Remediation.builder()
                .vulnerability(vulnerability)
                .status(RemediationStatus.planned)
                .build());

        remediationRepository.save(Remediation.builder()
                .vulnerability(vulnerability)
                .status(RemediationStatus.planned)
                .build());

        remediationRepository.save(Remediation.builder()
                .vulnerability(vulnerability)
                .status(RemediationStatus.in_progress)
                .build());

        assertThat(remediationRepository.countByStatus(RemediationStatus.planned)).isEqualTo(2);
        assertThat(remediationRepository.countByStatus(RemediationStatus.in_progress)).isEqualTo(1);
        assertThat(remediationRepository.countByStatus(RemediationStatus.completed)).isZero();
    }

    @Test
    void countOverdue_countsOnlyActiveOverdue() {
        remediationRepository.save(Remediation.builder()
                .vulnerability(vulnerability)
                .status(RemediationStatus.planned)
                .targetDate(LocalDate.now().minusDays(2))
                .build());

        remediationRepository.save(Remediation.builder()
                .vulnerability(vulnerability)
                .status(RemediationStatus.completed)
                .targetDate(LocalDate.now().minusDays(2))
                .completedAt(Instant.now())
                .completionMethod(CompletionMethod.manual)
                .completedBy("user1")
                .build());

        var activeStatuses = List.of(RemediationStatus.planned, RemediationStatus.in_progress);
        long count = remediationRepository.countOverdue(activeStatuses, LocalDate.now());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void avgCompletionDays_returnsAverageForCompleted() {
        remediationRepository.save(Remediation.builder()
                .vulnerability(vulnerability)
                .status(RemediationStatus.completed)
                .completionMethod(CompletionMethod.manual)
                .completedBy("user1")
                .completedAt(Instant.now())
                .build());

        // completedAt and createdAt are set nearly simultaneously, so avg is ~0
        Double avg = remediationRepository.avgCompletionDays();
        assertThat(avg).isNotNull();
    }

    @Test
    void avgCompletionDays_returnsNullWhenNoCompleted() {
        remediationRepository.save(Remediation.builder()
                .vulnerability(vulnerability)
                .status(RemediationStatus.planned)
                .build());

        Double avg = remediationRepository.avgCompletionDays();
        assertThat(avg).isNull();
    }
}
