package io.holbein.ephor.api.repositories;

import io.holbein.ephor.api.entity.RemediationComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RemediationCommentRepository extends JpaRepository<RemediationComment, Long> {

    List<RemediationComment> findByRemediationIdOrderByCreatedAtAsc(Long remediationId);
}
