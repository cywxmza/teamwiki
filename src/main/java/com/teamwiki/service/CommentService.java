package com.teamwiki.service;

import com.teamwiki.dto.*;
import com.teamwiki.entity.*;
import com.teamwiki.exception.BusinessException;
import com.teamwiki.repository.CommentRepository;
import com.teamwiki.repository.DocumentRepository;
import com.teamwiki.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentResponse createComment(Long documentId, CommentRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("文档不存在", HttpStatus.NOT_FOUND));

        Comment comment = Comment.builder()
                .content(request.getContent())
                .document(doc)
                .user(user)
                .selectedText(request.getSelectedText())
                .isInline(request.getIsInline() != null ? request.getIsInline() : false)
                .isResolved(false)
                .build();

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException("父评论不存在", HttpStatus.NOT_FOUND));
            comment.setParent(parent);
        }

        comment = commentRepository.save(comment);
        return toCommentResponse(comment);
    }

    public PageResponse<CommentResponse> getComments(Long documentId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> commentPage = commentRepository.findByDocumentIdAndParentIdIsNull(documentId, pageable);

        List<CommentResponse> comments = commentPage.getContent().stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());

        return PageResponse.<CommentResponse>builder()
                .content(comments)
                .totalElements(commentPage.getTotalElements())
                .totalPages(commentPage.getTotalPages())
                .currentPage(page)
                .pageSize(size)
                .build();
    }

    @Transactional
    public void resolveComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException("评论不存在", HttpStatus.NOT_FOUND));
        comment.setIsResolved(true);
        commentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }

    private CommentResponse toCommentResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .user(toUserDTO(comment.getUser()))
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .selectedText(comment.getSelectedText())
                .isInline(comment.getIsInline())
                .isResolved(comment.getIsResolved())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    private UserDTO toUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .department(user.getDepartment())
                .role(user.getRole().name())
                .enabled(user.getEnabled())
                .build();
    }
}
