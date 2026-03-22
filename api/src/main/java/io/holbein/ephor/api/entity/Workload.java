package io.holbein.ephor.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workloads", uniqueConstraints = {
        @UniqueConstraint(name = "unique_workload_identity",
                columnNames = {"namespace", "name", "kind"})
}, indexes = {
        @Index(name = "idx_workloads_identity", columnList = "namespace, name, kind"),
        @Index(name = "idx_workloads_last_scan", columnList = "last_scan_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id")
    private Scan scan;

    @Column(nullable = false)
    private String namespace;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private WorkloadKind kind;

    @Column(name = "created_at")
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_scan_id")
    private Scan lastScan;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "workload", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Container> containers = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "workload", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkloadLabel> labels = new ArrayList<>();

    @PrePersist
    private void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum WorkloadKind {
        Deployment, StatefulSet, DaemonSet, Pod, CronJob
    }
}
