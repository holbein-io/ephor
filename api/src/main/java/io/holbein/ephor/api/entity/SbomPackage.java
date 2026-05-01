package io.holbein.ephor.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "sbom_packages", indexes = {
        @Index(name = "idx_sbom_packages_name", columnList = "name"),
        @Index(name = "idx_sbom_packages_image", columnList = "image_reference")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SbomPackage {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sbom_id", nullable = false)
    private SbomDocument sbomDocument;

    @Column(name = "image_reference", nullable = false)
    private String imageReference;

    private String purl;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String version;

    private String type;

    private String license;

    private String supplier;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
