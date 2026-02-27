package io.holbein.ephor.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "triage_session_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TriageSessionMetrics {

    @Id
    @Column(name = "session_id")
    private Long sessionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "session_id")
    private TriageSession triageSession;

    @Builder.Default
    @Column(name = "total_vulnerabilities", nullable = false)
    private Integer totalVulnerabilities = 0;

    @Builder.Default
    @Column(name = "decisions_made", nullable = false)
    private Integer decisionsMade = 0;

    @Column(name = "bulk_operations_count")
    private Integer bulkOperationsCount;

    @Column(name = "individual_decisions_count")
    private Integer individualDecisionsCount;

    @Column(name = "prep_duration_minutes")
    private Integer prepDurationMinutes;

    @Column(name = "session_duration_minutes")
    private Integer sessionDurationMinutes;

    @Column(name = "efficiency_score", precision = 5, scale = 2)
    private BigDecimal efficiencyScore;

    @Column(name = "prep_completion_rate", precision = 5, scale = 2)
    private BigDecimal prepCompletionRate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "decision_breakdown", columnDefinition = "jsonb")
    private Map<String, Object> decisionBreakdown;

    @Column(name = "calculated_at")
    private Instant calculatedAt;

    // Note: triage_session_metrics does NOT have updated_at column, uses calculated_at

    @PrePersist
    @PreUpdate
    protected void onSave() {
        calculatedAt = Instant.now();
    }
}
