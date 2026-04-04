package io.holbein.ephor.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "sbom_documents", uniqueConstraints = {
        @UniqueConstraint(name = "unique_sbom_document", columnNames = {"image_reference", "content_hash"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SbomDocument {

    @Id
    private UUID id;

    @Column(name = "image_reference", nullable = false)
    private String imageReference;

    @Column(name = "image_digest")
    private String imageDigest;

    @Column(name = "content_hash", nullable = false)
    private String contentHash;

    @Column(nullable = false)
    private String format;

    @Column(name = "scan_group_id")
    private UUID scanGroupId;

    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen;

    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> document;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (firstSeen == null) {
            firstSeen = now;
        }
        if (lastSeen == null) {
            lastSeen = now;
        }
    }
}
