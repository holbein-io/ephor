package io.holbein.ephor.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "triage_bulk_operations", indexes = {
    @Index(name = "idx_triage_bulk_operations_session", columnList = "session_id"),
    @Index(name = "idx_triage_bulk_operations_plan", columnList = "bulk_plan_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TriageBulkOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private TriageSession triageSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bulk_plan_id")
    private TriageBulkPlan bulkPlan;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "vulnerability_ids", columnDefinition = "integer[]")
    private List<Integer> vulnerabilityIds;

    @Column(name = "operation_type", length = 50, nullable = false)
    private String operationType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "executed_by", nullable = false)
    private String executedBy;

    @Column(name = "executed_at")
    private Instant executedAt;

    // Note: triage_bulk_operations does NOT have updated_at column

    @PrePersist
    protected void onCreate() {
        if (executedAt == null) {
            executedAt = Instant.now();
        }
    }
}
