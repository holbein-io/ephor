package io.holbein.ephor.api.entity;

import io.holbein.ephor.api.model.enums.BulkAction;
import io.holbein.ephor.api.model.enums.BulkPlanStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "triage_bulk_plans", indexes = {
    @Index(name = "idx_triage_bulk_plans_session", columnList = "session_id"),
    @Index(name = "idx_triage_bulk_plans_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TriageBulkPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private TriageSession triageSession;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> filters;

    @Builder.Default
    @Column(name = "estimated_count")
    private Integer estimatedCount = 0;

    @Builder.Default
    @Column(name = "actual_count")
    private Integer actualCount = 0;

    @Builder.Default
    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private BulkPlanStatus status = BulkPlanStatus.planned;

    @Column(length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private BulkAction action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Builder.Default
    @Column(name = "created_during_prep")
    private Boolean createdDuringPrep = true;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "executed_by")
    private String executedBy;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    // Note: triage_bulk_plans does NOT have updated_at column

    @Builder.Default
    @OneToMany(mappedBy = "bulkPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TriageBulkOperation> operations = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

}
