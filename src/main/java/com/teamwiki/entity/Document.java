package com.teamwiki.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documents", indexes = {
    @Index(name = "idx_document_is_deleted", columnList = "isDeleted"),
    @Index(name = "idx_document_created_by", columnList = "created_by"),
    @Index(name = "idx_document_kb_id", columnList = "kb_id"),
    @Index(name = "idx_document_category_id", columnList = "category_id"),
    @Index(name = "idx_document_visibility", columnList = "visibility"),
    @Index(name = "idx_document_share_token", columnList = "shareToken"),
    @Index(name = "idx_document_deleted_at", columnList = "deletedAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String content = "";

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DocVisibility visibility = DocVisibility.PRIVATE;

    @Builder.Default
    private Boolean allowComment = true;

    @Builder.Default
    private Boolean isDeleted = false;

    @Builder.Default
    private Long viewCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kb_id")
    private KnowledgeBase knowledgeBase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_edited_by")
    private User lastEditedBy;

    @ManyToMany
    @JoinTable(
        name = "document_tags",
        joinColumns = @JoinColumn(name = "document_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private List<Tag> tags = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    private LocalDateTime publishedAt;

    @Column(length = 64, unique = true)
    private String shareToken;

    @Builder.Default
    private Boolean shareEnabled = false;

    public enum DocVisibility {
        PRIVATE, TEAM, PUBLIC
    }
}
