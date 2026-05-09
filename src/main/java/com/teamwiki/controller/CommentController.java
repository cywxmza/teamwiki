package com.teamwiki.controller;

import com.teamwiki.dto.*;
import com.teamwiki.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/documents/{documentId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<PageResponse<CommentResponse>> getComments(
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(commentService.getComments(documentId, page, size));
    }

    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long documentId,
            @RequestBody CommentRequest request,
            @RequestAttribute("username") String username) {
        return ResponseEntity.ok(commentService.createComment(documentId, request, username));
    }

    @PatchMapping("/{commentId}/resolve")
    public ResponseEntity<Void> resolveComment(@PathVariable Long documentId, @PathVariable Long commentId) {
        commentService.resolveComment(commentId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long documentId, @PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok().build();
    }
}
