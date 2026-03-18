package io.holbein.ephor.api.entity;

import io.holbein.ephor.api.model.enums.EscalationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "escalations", indexes = {
        @Index(name = "idx_escalations_status", columnList = "status"),
        @Index(name = "idx_escalations_priority", columnList = "priority"),
        @Index(name = "idx_escalations_due_date", columnList = "due_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Escalation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vulnerability_id")
    private Vulnerability vulnerability;

    @Builder.Default
    @Column(name = "escalation_level")
    private Integer escalationLevel = 1;

    @Column(name = "escalated_at")
    private Instant escalatedAt;

    @Column(name = "escalated_by")
    private String escalatedBy;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "ms_teams_message_id")
    private String msTeamsMessageId;

    @Builder.Default
    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private EscalationStatus status = EscalationStatus.pending;

    @Builder.Default
    @Column(length = 50)
    private String priority = "medium";

    @Column(name = "due_date")
    private Instant dueDate;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (escalatedAt == null) {
            escalatedAt = Instant.now();
        }
    }

}
