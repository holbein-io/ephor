package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.remediation.*;
import io.holbein.ephor.api.entity.Vulnerability;
import io.holbein.ephor.api.model.enums.CompletionMethod;
import io.holbein.ephor.api.model.enums.RemediationPriority;
import io.holbein.ephor.api.model.enums.RemediationStatus;
import io.holbein.ephor.api.model.enums.SeverityLevel;
import io.holbein.ephor.api.repositories.RemediationCommentRepository;
import io.holbein.ephor.api.repositories.RemediationRepository;
import io.holbein.ephor.api.repositories.VulnerabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemediationServiceTest extends BaseIntegrationTest {

    @Autowired
    private RemediationService remediationService;

    @Autowired
    private RemediationRepository remediationRepository;

    @Autowired
    private RemediationCommentRepository remediationCommentRepository;

    @Autowired
    private VulnerabilityRepository vulnerabilityRepository;

    private Vulnerability vulnerability;

    @BeforeEach
    void setUp() {
        remediationCommentRepository.deleteAll();
        remediationRepository.deleteAll();
        vulnerabilityRepository.deleteAll();

        vulnerability = vulnerabilityRepository.save(Vulnerability.builder()
                .cveId("CVE-2025-1000")
                .packageName("openssl")
                .packageVersion("3.0.1")
                .severity(SeverityLevel.CRITICAL)
                .scannerType("trivy")
                .build());
    }

    @Test
    void createRemediation_setsDefaultStatus() {
        var request = new CreateRemediationRequest(
                vulnerability.getId(), "alice", LocalDate.now().plusDays(30),
                RemediationPriority.high, "Fix ASAP");

        RemediationResponse response = remediationService.createRemediation(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo(RemediationStatus.planned);
        assertThat(response.assignedTo()).isEqualTo("alice");
        assertThat(response.priority()).isEqualTo(RemediationPriority.high);
    }

    @Test
    void changeStatus_plannedToInProgress() {
        RemediationResponse created = createDefaultRemediation();

        var statusRequest = new ChangeRemediationStatusRequest(
                RemediationStatus.in_progress, null, null, null);
        RemediationResponse updated = remediationService.changeRemediationStatus(created.id(), statusRequest);

        assertThat(updated.status()).isEqualTo(RemediationStatus.in_progress);
    }

    @Test
    void changeStatus_inProgressToCompleted() {
        RemediationResponse created = createDefaultRemediation();

        remediationService.changeRemediationStatus(created.id(),
                new ChangeRemediationStatusRequest(RemediationStatus.in_progress, null, null, null));

        RemediationResponse completed = remediationService.changeRemediationStatus(created.id(),
                new ChangeRemediationStatusRequest(RemediationStatus.completed,
                        CompletionMethod.manual, "alice", null));

        assertThat(completed.status()).isEqualTo(RemediationStatus.completed);
        assertThat(completed.completedAt()).isNotNull();
        assertThat(completed.completionMethod()).isEqualTo(CompletionMethod.manual);
        assertThat(completed.completedBy()).isEqualTo("alice");
    }

    @Test
    void changeStatus_plannedToAbandoned_requiresNotes() {
        RemediationResponse created = createDefaultRemediation();

        assertThatThrownBy(() ->
                remediationService.changeRemediationStatus(created.id(),
                        new ChangeRemediationStatusRequest(RemediationStatus.abandoned, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("notes");
    }

    @Test
    void changeStatus_plannedToAbandoned_withNotes() {
        RemediationResponse created = createDefaultRemediation();

        RemediationResponse abandoned = remediationService.changeRemediationStatus(created.id(),
                new ChangeRemediationStatusRequest(RemediationStatus.abandoned, null, null, "No longer relevant"));

        assertThat(abandoned.status()).isEqualTo(RemediationStatus.abandoned);
        assertThat(abandoned.completedAt()).isNotNull();
    }

    @Test
    void changeStatus_completedToAnything_throwsIllegalState() {
        RemediationResponse created = createDefaultRemediation();

        remediationService.changeRemediationStatus(created.id(),
                new ChangeRemediationStatusRequest(RemediationStatus.in_progress, null, null, null));
        remediationService.changeRemediationStatus(created.id(),
                new ChangeRemediationStatusRequest(RemediationStatus.completed,
                        CompletionMethod.manual, "alice", null));

        assertThatThrownBy(() ->
                remediationService.changeRemediationStatus(created.id(),
                        new ChangeRemediationStatusRequest(RemediationStatus.in_progress, null, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition");
    }

    @Test
    void changeStatus_inProgressToCompleted_requiresCompletionMethod() {
        RemediationResponse created = createDefaultRemediation();

        remediationService.changeRemediationStatus(created.id(),
                new ChangeRemediationStatusRequest(RemediationStatus.in_progress, null, null, null));

        assertThatThrownBy(() ->
                remediationService.changeRemediationStatus(created.id(),
                        new ChangeRemediationStatusRequest(RemediationStatus.completed, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("completionMethod");
    }

    @Test
    void changeStatus_inProgressToCompleted_requiresCompletedBy() {
        RemediationResponse created = createDefaultRemediation();

        remediationService.changeRemediationStatus(created.id(),
                new ChangeRemediationStatusRequest(RemediationStatus.in_progress, null, null, null));

        assertThatThrownBy(() ->
                remediationService.changeRemediationStatus(created.id(),
                        new ChangeRemediationStatusRequest(RemediationStatus.completed,
                                CompletionMethod.manual, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("completedBy");
    }

    @Test
    void changeStatus_createsSystemComment() {
        RemediationResponse created = createDefaultRemediation();

        remediationService.changeRemediationStatus(created.id(),
                new ChangeRemediationStatusRequest(RemediationStatus.in_progress, null, null, null));

        List<RemediationCommentResponse> comments = remediationService.getComments(created.id());
        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).author()).isEqualTo("system");
        assertThat(comments.get(0).comment()).contains("planned").contains("in_progress");
    }

    @Test
    void changeStatus_plannedToCompleted_isInvalidTransition() {
        RemediationResponse created = createDefaultRemediation();

        assertThatThrownBy(() ->
                remediationService.changeRemediationStatus(created.id(),
                        new ChangeRemediationStatusRequest(RemediationStatus.completed,
                                CompletionMethod.manual, "alice", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition");
    }

    private RemediationResponse createDefaultRemediation() {
        return remediationService.createRemediation(
                new CreateRemediationRequest(
                        vulnerability.getId(), "alice",
                        LocalDate.now().plusDays(30),
                        RemediationPriority.high, "test remediation"));
    }
}
