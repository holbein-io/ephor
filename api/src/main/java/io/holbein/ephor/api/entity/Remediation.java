package io.holbein.ephor.api.entity;

import io.holbein.ephor.api.model.enums.CompletionMethod;
import io.holbein.ephor.api.model.enums.RemediationPriority;
import io.holbein.ephor.api.model.enums.RemediationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "remediations", indexes = {
    @Index(name = "idx_remediations_vuln", columnList = "vulnerability_id"),
    @Index(name = "idx_remediations_status", columnList = "status"),
    @Index(name = "idx_remediations_assigned", columnList = "assigned_to"),
    @Index(name = "idx_remediations_target_date", columnList = "target_date"),
    @Index(name = "idx_remediations_triage_decision", columnList = "triage_decision_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Remediation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vulnerability_id", nullable = false)
    private Vulnerability vulnerability;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triage_decision_id")
    private TriageDecision triageDecision;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private RemediationPriority priority;

    @Builder.Default
    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private RemediationStatus status = RemediationStatus.planned;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completion_method", length = 50)
    @Enumerated(EnumType.STRING)
    private CompletionMethod completionMethod;

    @Column(name = "completed_by")
    private String completedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "remediation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RemediationComment> comments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

}
