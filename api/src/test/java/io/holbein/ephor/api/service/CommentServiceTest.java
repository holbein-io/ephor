package io.holbein.ephor.api.service;

import io.holbein.ephor.api.auth.UserContext;
import io.holbein.ephor.api.auth.UserContextHolder;
import io.holbein.ephor.api.entity.Comment;
import io.holbein.ephor.api.exception.ForbiddenException;
import io.holbein.ephor.api.exception.ResourceNotFoundException;
import io.holbein.ephor.api.model.enums.EntityType;
import io.holbein.ephor.api.repositories.CommentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentServiceTest extends BaseIntegrationTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clearContext();
    }

    @Test
    void addComment_setsEntityTypeAndBody() {
        setUser("alice", List.of("ephor-analysts"));

        Comment comment = commentService.addComment(EntityType.VULNERABILITY, 1L, "Needs investigation");

        assertThat(comment.getId()).isNotNull();
        assertThat(comment.getEntityType()).isEqualTo(EntityType.VULNERABILITY);
        assertThat(comment.getEntityId()).isEqualTo(1L);
        assertThat(comment.getBody()).isEqualTo("Needs investigation");
        assertThat(comment.getCreatedBy()).isEqualTo("alice");
    }

    @Test
    void getComments_returnsByEntityTypeAndId() {
        setUser("alice", List.of("ephor-analysts"));

        commentService.addComment(EntityType.VULNERABILITY, 1L, "Comment on vuln 1");
        commentService.addComment(EntityType.VULNERABILITY, 1L, "Another comment on vuln 1");
        commentService.addComment(EntityType.ESCALATION, 2L, "Comment on escalation 2");

        List<Comment> vulnComments = commentService.getComments(EntityType.VULNERABILITY, 1L);
        assertThat(vulnComments).hasSize(2);

        List<Comment> escalationComments = commentService.getComments(EntityType.ESCALATION, 2L);
        assertThat(escalationComments).hasSize(1);

        List<Comment> emptyComments = commentService.getComments(EntityType.VULNERABILITY, 999L);
        assertThat(emptyComments).isEmpty();
    }

    @Test
    void updateComment_ownerCanUpdate() {
        setUser("alice", List.of("ephor-analysts"));
        Comment comment = commentService.addComment(EntityType.VULNERABILITY, 1L, "Original text");

        Comment updated = commentService.updateComment(comment.getId(), "Updated text");

        assertThat(updated.getBody()).isEqualTo("Updated text");
    }

    @Test
    void updateComment_nonOwnerForbidden() {
        setUser("alice", List.of("ephor-analysts"));
        Comment comment = commentService.addComment(EntityType.VULNERABILITY, 1L, "Alice's comment");

        setUser("bob", List.of("ephor-analysts"));

        assertThatThrownBy(() -> commentService.updateComment(comment.getId(), "Bob's edit"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("your own comments");
    }

    @Test
    void updateComment_adminCanUpdateAnyComment() {
        setUser("alice", List.of("ephor-analysts"));
        Comment comment = commentService.addComment(EntityType.VULNERABILITY, 1L, "Alice's comment");

        setUser("admin-user", List.of("ephor-admins"));

        Comment updated = commentService.updateComment(comment.getId(), "Admin override");
        assertThat(updated.getBody()).isEqualTo("Admin override");
    }

    @Test
    void deleteComment_ownerCanDelete() {
        setUser("alice", List.of("ephor-analysts"));
        Comment comment = commentService.addComment(EntityType.VULNERABILITY, 1L, "To be deleted");

        commentService.deleteComment(comment.getId());

        assertThat(commentRepository.findById(comment.getId())).isEmpty();
    }

    @Test
    void deleteComment_nonOwnerForbidden() {
        setUser("alice", List.of("ephor-analysts"));
        Comment comment = commentService.addComment(EntityType.VULNERABILITY, 1L, "Alice's comment");

        setUser("bob", List.of("ephor-analysts"));

        assertThatThrownBy(() -> commentService.deleteComment(comment.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deleteComment_adminCanDeleteAnyComment() {
        setUser("alice", List.of("ephor-analysts"));
        Comment comment = commentService.addComment(EntityType.VULNERABILITY, 1L, "Alice's comment");

        setUser("admin-user", List.of("ephor-admins"));

        commentService.deleteComment(comment.getId());
        assertThat(commentRepository.findById(comment.getId())).isEmpty();
    }

    @Test
    void updateComment_notFound_throwsException() {
        setUser("alice", List.of("ephor-analysts"));

        assertThatThrownBy(() -> commentService.updateComment(999L, "no such comment"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteComment_notFound_throwsException() {
        setUser("alice", List.of("ephor-analysts"));

        assertThatThrownBy(() -> commentService.deleteComment(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void setUser(String username, List<String> groups) {
        UserContextHolder.setContext(new UserContext(username, username + "@test.com", groups, username, null));
    }
}
