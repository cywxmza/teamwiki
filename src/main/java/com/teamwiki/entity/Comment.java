package com.teamwiki.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_comment_document_id", columnList = "document_id"),
    @Index(name = "idx_comment_user_id", columnList = "user_id"),
    @Index(name = "idx_comment_parent_id", columnList = "parent_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Column(name = "is_resolved")
    @Builder.Default
    private Boolean isResolved = false;

    @Column(name = "selected_text", length = 500)
    private String selectedText;

    @Builder.Default
    private Boolean isInline = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
