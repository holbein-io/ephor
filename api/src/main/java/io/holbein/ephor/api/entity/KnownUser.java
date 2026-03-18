package io.holbein.ephor.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "known_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnownUser {

    @Id
    @Column(length = 255)
    private String username;

    @Column(length = 255)
    private String email;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "groups_csv", length = 1000)
    private String groupsCsv;

    @Column(name = "first_seen_at")
    private Instant firstSeenAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;
}
