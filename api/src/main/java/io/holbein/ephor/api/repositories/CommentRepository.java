package io.holbein.ephor.api.repositories;

import io.holbein.ephor.api.entity.Comment;
import io.holbein.ephor.api.model.enums.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c WHERE c.vulnerability.id = :vulnerabilityId ORDER BY c.createdAt DESC")
    List<Comment> findByVulnerabilityIdOrderByCreatedAtDesc(@Param("vulnerabilityId") long vulnerabilityId);

    @Query("SELECT c FROM Comment c WHERE c.id = :commentId AND c.vulnerability.id = :vulnerabilityId")
    Optional<Comment> findByIdAndVulnerabilityId(@Param("commentId") long commentId,
                                                  @Param("vulnerabilityId") long vulnerabilityId);

    List<Comment> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType entityType, Long entityId);

    List<Comment> findByCreatedByOrderByCreatedAtDesc(String createdBy);
}
