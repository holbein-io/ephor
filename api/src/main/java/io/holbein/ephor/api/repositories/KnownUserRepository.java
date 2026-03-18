package io.holbein.ephor.api.repositories;

import io.holbein.ephor.api.entity.KnownUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnownUserRepository extends JpaRepository<KnownUser, String> {

    @Query(value = """
            SELECT * FROM known_users
            WHERE LOWER(username) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(email) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(display_name) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY last_seen_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<KnownUser> search(@Param("query") String query, @Param("limit") int limit);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO known_users (username, email, display_name, groups_csv, first_seen_at, last_seen_at)
            VALUES (:username, :email, :displayName, :groupsCsv, NOW(), NOW())
            ON CONFLICT (username) DO UPDATE SET
                email = EXCLUDED.email,
                display_name = EXCLUDED.display_name,
                groups_csv = EXCLUDED.groups_csv,
                last_seen_at = NOW()
            """, nativeQuery = true)
    void upsert(@Param("username") String username,
                @Param("email") String email,
                @Param("displayName") String displayName,
                @Param("groupsCsv") String groupsCsv);
}
