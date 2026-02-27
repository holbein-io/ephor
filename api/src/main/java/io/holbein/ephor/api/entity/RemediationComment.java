package io.holbein.ephor.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "remediation_comments", indexes = {
    @Index(name = "idx_remediation_comments_remediation_id", columnList = "remediation_id"),
    @Index(name = "idx_remediation_comments_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemediationComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "remediation_id", nullable = false)
    private Remediation remediation;

    @Builder.Default
    @Column(nullable = false)
    private String author = "system";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at")
    private Instant createdAt;

    // Note: remediation_comments is append-only, no updated_at column

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
