package io.holbein.ephor.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Legacy notification settings table.
 * Note: This table is deprecated and replaced by SystemSettings.
 * Kept for backwards compatibility during migration.
 */
@Entity
@Table(name = "notification_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Deprecated
public class NotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_name", length = 100, nullable = false, unique = true)
    private String settingName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "setting_value", nullable = false, columnDefinition = "jsonb")
    private Object settingValue;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // Note: notification_settings does NOT have created_at column

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = Instant.now();
    }
}
