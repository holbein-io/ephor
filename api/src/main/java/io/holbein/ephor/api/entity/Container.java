package io.holbein.ephor.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "containers", uniqueConstraints = {
        @UniqueConstraint(name = "unique_container_identity",
                columnNames = {"workload_id", "name"})
}, indexes = {
        @Index(name = "idx_containers_workload", columnList = "workload_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Container {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workload_id")
    private Workload workload;

    @Builder.Default
    @OneToMany(mappedBy = "container", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VulnerabilityInstance> vulnerabilityInstances = new ArrayList<>();

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "image_name", length = 500)
    private String imageName;

    @Column(name = "image_tag")
    private String imageTag;

    @Column(name = "image_created")
    private Instant imageCreated;

    @Column(name = "base_image_created")
    private Instant baseImageCreated;

    @Column(name = "detected_ecosystems", columnDefinition = "jsonb")
    @JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private List<String> detectedEcosystems;

    @Column(name = "os_family", length = 50)
    private String osFamily;

    @Column(name = "os_name", length = 50)
    private String osName;

    @Column(name = "repo_digests", columnDefinition = "jsonb")
    @JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private List<String> repoDigests;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
