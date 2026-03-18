package io.holbein.ephor.api.service;

import io.holbein.ephor.api.auth.UserContextHolder;
import io.holbein.ephor.api.entity.Comment;
import io.holbein.ephor.api.exception.ForbiddenException;
import io.holbein.ephor.api.exception.ResourceNotFoundException;
import io.holbein.ephor.api.model.enums.AuditAction;
import io.holbein.ephor.api.model.enums.EntityType;
import io.holbein.ephor.api.model.enums.Permission;
import io.holbein.ephor.api.model.enums.Role;
import io.holbein.ephor.api.repositories.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<Comment> getComments(EntityType entityType, Long entityId) {
        return commentRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(entityType, entityId);
    }

    @Transactional
    public Comment addComment(EntityType entityType, Long entityId, String body) {
        Comment comment = Comment.builder()
                .entityType(entityType)
                .entityId(entityId)
                .body(body)
                .build();

        Comment saved = commentRepository.save(comment);

        auditService.log(AuditAction.COMMENT_ADDED, entityType, entityId,
                Map.of("commentId", saved.getId()));

        return saved;
    }

    @Transactional
    public Comment updateComment(Long commentId, String body) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> ResourceNotFoundException.comment(commentId));

        checkOwnership(comment);

        comment.setBody(body);
        Comment updated = commentRepository.save(comment);

        auditService.log(AuditAction.COMMENT_UPDATED, comment.getEntityType(), comment.getEntityId(),
                Map.of("commentId", commentId));

        return updated;
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> ResourceNotFoundException.comment(commentId));

        checkOwnership(comment);

        auditService.log(AuditAction.COMMENT_DELETED, comment.getEntityType(), comment.getEntityId(),
                Map.of("commentId", commentId));

        commentRepository.delete(comment);
    }

    private void checkOwnership(Comment comment) {
        String currentUser = UserContextHolder.getUsername("anonymous");
        if (currentUser.equals(comment.getCreatedBy())) {
            return;
        }

        Set<Permission> permissions = UserContextHolder.getContext()
                .map(ctx -> Role.resolvePermissions(ctx.groups()))
                .orElse(Set.of());

        if (!permissions.contains(Permission.MANAGE_ADMIN)) {
            throw new ForbiddenException("You can only modify your own comments");
        }
    }
}
