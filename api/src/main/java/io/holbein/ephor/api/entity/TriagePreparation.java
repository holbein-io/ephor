package io.holbein.ephor.api.entity;

import io.holbein.ephor.api.model.enums.PrepStatus;
import io.holbein.ephor.api.model.enums.PreliminaryDecision;
import io.holbein.ephor.api.model.enums.PriorityFlag;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "triage_preparations", uniqueConstraints = {
    @UniqueConstraint(name = "unique_prep_vuln_session",
        columnNames = {"session_id", "vulnerability_id"})
}, indexes = {
    @Index(name = "idx_triage_preparations_session", columnList = "session_id"),
    @Index(name = "idx_triage_preparations_vuln", columnList = "vulnerability_id"),
    @Index(name = "idx_triage_preparations_status", columnList = "prep_status"),
    @Index(name = "idx_triage_preparations_priority", columnList = "priority_flag")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TriagePreparation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private TriageSession triageSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vulnerability_id")
    private Vulnerability vulnerability;

    @Builder.Default
    @Column(name = "prep_status", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private PrepStatus prepStatus = PrepStatus.pending;

    @Column(name = "prep_notes", columnDefinition = "TEXT")
    private String prepNotes;

    @Column(name = "preliminary_decision", length = 50)
    @Enumerated(EnumType.STRING)
    private PreliminaryDecision preliminaryDecision;

    @Builder.Default
    @Column(name = "priority_flag", length = 20)
    @Enumerated(EnumType.STRING)
    private PriorityFlag priorityFlag = PriorityFlag.medium;

    @Column(name = "prep_by", nullable = false)
    private String prepBy;

    @Column(name = "prep_at")
    private Instant prepAt;

    // Note: triage_preparations does NOT have updated_at column
    @PrePersist
    protected void onCreate() {
        if (prepAt == null) {
            prepAt = Instant.now();
        }
    }

}
