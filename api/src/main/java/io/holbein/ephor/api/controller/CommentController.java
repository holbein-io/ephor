package io.holbein.ephor.api.controller;

import io.holbein.ephor.api.auth.RequireAuth;
import io.holbein.ephor.api.dto.comment.CommentResponse;
import io.holbein.ephor.api.dto.comment.CreateCommentRequest;
import io.holbein.ephor.api.entity.Comment;
import io.holbein.ephor.api.model.enums.EntityType;
import io.holbein.ephor.api.model.enums.Permission;
import io.holbein.ephor.api.service.CommentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Polymorphic comments for any entity")
@RequireAuth
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/vulnerabilities/{id}")
    public List<CommentResponse> getVulnerabilityComments(@PathVariable long id) {
        return commentService.getComments(EntityType.VULNERABILITY, id).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @PostMapping("/vulnerabilities/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireAuth(permissions = {Permission.MANAGE_VULNERABILITIES})
    public CommentResponse addVulnerabilityComment(@PathVariable long id,
                                                   @Valid @RequestBody CreateCommentRequest request) {
        Comment comment = commentService.addComment(EntityType.VULNERABILITY, id, request.body());
        return CommentResponse.from(comment);
    }

    @GetMapping("/escalations/{id}")
    public List<CommentResponse> getEscalationComments(@PathVariable long id) {
        return commentService.getComments(EntityType.ESCALATION, id).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @PostMapping("/escalations/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireAuth(permissions = {Permission.MANAGE_ESCALATIONS})
    public CommentResponse addEscalationComment(@PathVariable long id,
                                                @Valid @RequestBody CreateCommentRequest request) {
        Comment comment = commentService.addComment(EntityType.ESCALATION, id, request.body());
        return CommentResponse.from(comment);
    }

    @PutMapping("/{id}")
    public CommentResponse updateComment(@PathVariable long id,
                                         @Valid @RequestBody CreateCommentRequest request) {
        Comment comment = commentService.updateComment(id, request.body());
        return CommentResponse.from(comment);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable long id) {
        commentService.deleteComment(id);
    }
}
