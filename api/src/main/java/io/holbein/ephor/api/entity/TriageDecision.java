package io.holbein.ephor.api.entity;

import io.holbein.ephor.api.model.enums.DecisionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "triage_decisions", indexes = {
        @Index(name = "idx_triage_decisions_session", columnList = "session_id"),
        @Index(name = "idx_triage_decisions_vuln", columnList = "vulnerability_id"),
        @Index(name = "idx_triage_decisions_decision", columnList = "decision")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TriageDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private TriageSession triageSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vulnerability_id")
    private Vulnerability vulnerability;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private DecisionStatus decision;

    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "decided_by")
    private String decidedBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @OneToOne(mappedBy = "triageDecision", cascade = CascadeType.ALL)
    private Remediation remediation;

    @PrePersist
    protected  void onCreate() {
        this.createdAt = Instant.now();
    }

    public DecisionStatus getStatus() {
        return decision;
    }

    public void setStatus(DecisionStatus status) {
        this.decision = status;
    }
}
