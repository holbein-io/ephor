package io.holbein.ephor.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "workload_labels", uniqueConstraints = {
        @UniqueConstraint(name = "unique_workload_label", columnNames = {"workload_id", "label_key"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkloadLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workload_id")
    private Workload workload;

    @Column(name = "label_key", nullable = false, length = 100)
    private String labelKey;

    @Column(name = "label_value", nullable = false, length = 255)
    private String labelValue;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    private void onCreate() {
        createdAt = Instant.now();
    }
}
