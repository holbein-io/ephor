package io.holbein.ephor.api.entity;

import io.holbein.ephor.api.model.enums.ScanStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "scans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String namespace;

    @Column(name = "scan_label", nullable = false)
    private String scanLabel;

    @Column(name = "scan_group_id")
    private UUID scanGroupId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ScanStatus status;

    @Column(name = "started_at")
    private Instant startedAt; // ISO 8601 String

    @Column(name = "completed_at")
    private Instant completedAt; // ISO 8601 String

    @Column(name = "trivy_version", length = 50)
    private String trivyVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scan_config", columnDefinition = "jsonb")
    private Map<String, Object> scanConfig;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "scan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Workload> workloads = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "scan",cascade = CascadeType.ALL)
    private List<VulnerabilityInstance> vulnerabilityInstances = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        updatedAt = Instant.now();
        if (startedAt == null) {
            startedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

}
