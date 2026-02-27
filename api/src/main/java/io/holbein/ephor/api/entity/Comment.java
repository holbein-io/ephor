package io.holbein.ephor.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vulnerability_id")
    private Vulnerability vulnerability;

    @Column(length = 255)
    private String author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    @Builder.Default
    @Column(name = "comment_type", length = 50)
    @Enumerated(EnumType.STRING)
    private CommentType commentType = CommentType.triage;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public enum CommentType {
        triage, escalation, resolution
    }
}
