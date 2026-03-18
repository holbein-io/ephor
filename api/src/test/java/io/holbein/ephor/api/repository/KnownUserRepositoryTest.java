package io.holbein.ephor.api.repository;

import io.holbein.ephor.api.entity.KnownUser;
import io.holbein.ephor.api.repositories.KnownUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class KnownUserRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private KnownUserRepository knownUserRepository;

    @BeforeEach
    void setUp() {
        knownUserRepository.deleteAll();
    }

    @Test
    void upsert_insertsNewUser() {
        knownUserRepository.upsert("alice", "alice@example.com", "Alice Smith", "ephor-analysts,developers");

        Optional<KnownUser> found = knownUserRepository.findById("alice");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@example.com");
        assertThat(found.get().getDisplayName()).isEqualTo("Alice Smith");
        assertThat(found.get().getGroupsCsv()).isEqualTo("ephor-analysts,developers");
        assertThat(found.get().getFirstSeenAt()).isNotNull();
        assertThat(found.get().getLastSeenAt()).isNotNull();
    }

    @Test
    void upsert_updatesExistingUser() {
        knownUserRepository.upsert("alice", "alice@old.com", "Alice", "ephor-viewers");

        KnownUser first = knownUserRepository.findById("alice").orElseThrow();
        var firstSeenAt = first.getFirstSeenAt();
        var firstLastSeenAt = first.getLastSeenAt();

        // Second upsert with updated email and groups
        knownUserRepository.upsert("alice", "alice@new.com", "Alice Smith", "ephor-analysts,ephor-leads");

        KnownUser updated = knownUserRepository.findById("alice").orElseThrow();
        assertThat(updated.getEmail()).isEqualTo("alice@new.com");
        assertThat(updated.getDisplayName()).isEqualTo("Alice Smith");
        assertThat(updated.getGroupsCsv()).isEqualTo("ephor-analysts,ephor-leads");
        assertThat(updated.getFirstSeenAt()).isEqualTo(firstSeenAt);
        assertThat(updated.getLastSeenAt()).isAfterOrEqualTo(firstLastSeenAt);
    }

    @Test
    void upsert_preservesFirstSeenAt() {
        knownUserRepository.upsert("bob", "bob@example.com", "Bob", "ephor-viewers");
        var firstSeen = knownUserRepository.findById("bob").orElseThrow().getFirstSeenAt();

        // Multiple upserts should not change first_seen_at
        knownUserRepository.upsert("bob", "bob@example.com", "Bob Jones", "ephor-analysts");
        knownUserRepository.upsert("bob", "bob@example.com", "Bob Jones Jr", "ephor-admins");

        var afterMultipleUpserts = knownUserRepository.findById("bob").orElseThrow();
        assertThat(afterMultipleUpserts.getFirstSeenAt()).isEqualTo(firstSeen);
        assertThat(afterMultipleUpserts.getDisplayName()).isEqualTo("Bob Jones Jr");
    }

    @Test
    void upsert_handlesNullEmail() {
        knownUserRepository.upsert("svc-account", null, "Service Account", "");

        Optional<KnownUser> found = knownUserRepository.findById("svc-account");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isNull();
    }

    @Test
    void upsert_multipleUsers_independent() {
        knownUserRepository.upsert("alice", "alice@example.com", "Alice", "ephor-analysts");
        knownUserRepository.upsert("bob", "bob@example.com", "Bob", "ephor-viewers");

        assertThat(knownUserRepository.findAll()).hasSize(2);
        assertThat(knownUserRepository.findById("alice").orElseThrow().getGroupsCsv()).isEqualTo("ephor-analysts");
        assertThat(knownUserRepository.findById("bob").orElseThrow().getGroupsCsv()).isEqualTo("ephor-viewers");
    }
}
