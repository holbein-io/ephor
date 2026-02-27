package io.holbein.ephor.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "system_settings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"category", "setting_key"})
}, indexes = {
    @Index(name = "idx_system_settings_category", columnList = "category"),
    @Index(name = "idx_system_settings_key", columnList = "setting_key")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String category;

    @Column(name = "setting_key", length = 100, nullable = false)
    private String settingKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "setting_value", nullable = false, columnDefinition = "jsonb")
    private Object settingValue;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_value", columnDefinition = "jsonb")
    private Object defaultValue;

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
