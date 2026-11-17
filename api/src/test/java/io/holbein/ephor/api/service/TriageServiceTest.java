package io.holbein.ephor.api.service;

import io.holbein.ephor.api.dto.triage.decision.CreateDecisionRequest;
import io.holbein.ephor.api.dto.triage.preparation.AddPreparationRequest;
import io.holbein.ephor.api.dto.triage.session.ChangeSessionStatusRequest;
import io.holbein.ephor.api.dto.triage.session.CreateSessionRequest;
import io.holbein.ephor.api.dto.triage.session.SessionResponse;
import io.holbein.ephor.api.dto.triage.shared.DeleteResponse;
import io.holbein.ephor.api.entity.Remediation;
import io.holbein.ephor.api.entity.Vulnerability;
import io.holbein.ephor.api.model.enums.AuditAction;
import io.holbein.ephor.api.model.enums.DecisionStatus;
import io.holbein.ephor.api.model.enums.PriorityFlag;
import io.holbein.ephor.api.model.enums.RemediationPriority;
import io.holbein.ephor.api.model.enums.SessionStatus;
import io.holbein.ephor.api.model.enums.SeverityLevel;
import io.holbein.ephor.api.repositories.AuditLogRepository;
import io.holbein.ephor.api.repositories.RemediationCommentRepository;
import io.holbein.ephor.api.repositories.RemediationRepository;
import io.holbein.ephor.api.repositories.TriageDecisionRepository;
import io.holbein.ephor.api.repositories.TriagePreparationsRepository;
import io.holbein.ephor.api.repositories.TriageSessionRepository;
import io.holbein.ephor.api.repositories.VulnerabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TriageServiceTest extends BaseIntegrationTest {

    @Autowired
    private TriageService triageService;

    @Autowired
    private TriageSessionRepository triageSessionRepository;

    @Autowired
    private TriagePreparationsRepository triagePreparationsRepository;

    @Autowired
    private TriageDecisionRepository triageDecisionRepository;

    @Autowired
    private RemediationRepository remediationRepository;

    @Autowired
    private RemediationCommentRepository remediationCommentRepository;

    @Autowired
    private VulnerabilityRepository vulnerabilityRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private Vulnerability vulnerability;

    @BeforeEach
    void setUp() {
        remediationCommentRepository.deleteAll();
        remediationRepository.deleteAll();
        triageSessionRepository.deleteAll();
        vulnerabilityRepository.deleteAll();
        auditLogRepository.deleteAll();

        vulnerability = vulnerabilityRepository.save(Vulnerability.builder()
                .cveId("CVE-2025-2000")
                .packageName("curl")
                .packageVersion("8.5.0")
                .severity(SeverityLevel.HIGH)
                .scannerType("trivy")
                .build());
    }

    @Test
    void deleteTriageSession_removesSessionChildrenButKeepsRemediation() {
        SessionResponse session = activeSessionWithDecision(DecisionStatus.needs_remediation);
        List<Remediation> before = remediationRepository.findAllByVulnerabilityId(vulnerability.getId());
        assertThat(before).hasSize(1);
        assertThat(before.getFirst().getTriageDecision()).isNotNull();

        DeleteResponse response = triageService.deleteTriageSession(session.id());

        assertThat(response.success()).isTrue();
        assertThat(triageSessionRepository.findById(session.id())).isEmpty();
        assertThat(triagePreparationsRepository.findAll()).isEmpty();
        assertThat(triageDecisionRepository.findAll()).isEmpty();

        List<Remediation> after = remediationRepository.findAllByVulnerabilityId(vulnerability.getId());
        assertThat(after).hasSize(1);
        assertThat(after.getFirst().getId()).isEqualTo(before.getFirst().getId());
        assertThat(after.getFirst().getTriageDecision()).isNull();
    }

    @Test
    void deleteTriageSession_writesAuditEntry() {
        SessionResponse session = activeSessionWithDecision(DecisionStatus.accepted_risk);

        triageService.deleteTriageSession(session.id());

        assertThat(auditLogRepository.findAll())
                .anySatisfy(entry -> {
                    assertThat(entry.getAction()).isEqualTo(AuditAction.TRIAGE_SESSION_DELETED);
                    assertThat(entry.getEntityId()).isEqualTo(session.id());
                    assertThat(entry.getDetails()).contains("\"decisionsRemoved\": 1");
                });
    }

    @Test
    void deleteTriageSession_unknownIdReportsFailure() {
        DeleteResponse response = triageService.deleteTriageSession(999_999L);

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("not found");
    }

    private SessionResponse activeSessionWithDecision(DecisionStatus decision) {
        SessionResponse session = triageService.createTriageSession(
                new CreateSessionRequest(LocalDate.now(), "alice", null, List.of("alice"), null));
        triageService.changeSessionStatus(session.id(),
                new ChangeSessionStatusRequest(SessionStatus.ACTIVE, "alice", null, null));
        triageService.createPreparation(new AddPreparationRequest(
                session.id(), vulnerability.getId(), null, PriorityFlag.high, "alice"));
        triageService.createDecision(new CreateDecisionRequest(
                session.id(), vulnerability.getId(), decision, "decided in test", "alice",
                "bob", LocalDate.now().plusDays(14), RemediationPriority.high));
        return session;
    }
}
