package io.holbein.ephor.api.entity;

import io.holbein.ephor.api.model.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "triage_sessions", indexes = {
        @Index(name = "idx_triage_sessions_date", columnList = "session_date"),
        @Index(name = "idx_triage_sessions_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TriageSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private String[] attendees;

    @Builder.Default
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.PREPARING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "prep_completed_at")
    private Instant prepCompletedAt;

    @Column(name = "session_started_at")
    private Instant sessionStartedAt;

    @Column(name = "prep_lead")
    private String prepLead;

    @Column(name = "prep_notes", columnDefinition = "TEXT")
    private String prepNotes;

    @Column(name = "session_duration_minutes")
    private Integer sessionDurationMinutes;

    @Column(name = "prep_duration_minutes")
    private Integer prepDurationMinutes;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "triageSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TriageDecision> decisions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "triageSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TriagePreparation> preparations = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "triageSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TriageBulkPlan> bulkPlans = new ArrayList<>();

    @OneToOne(mappedBy = "triageSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private TriageSessionMetrics metrics;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

}
