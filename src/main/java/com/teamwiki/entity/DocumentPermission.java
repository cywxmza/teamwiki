package com.teamwiki.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_permissions", indexes = {
    @Index(name = "idx_perm_document_id", columnList = "document_id"),
    @Index(name = "idx_perm_user_id", columnList = "user_id"),
    @Index(name = "idx_perm_doc_user", columnList = "document_id, user_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PermissionLevel permissionLevel;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum PermissionLevel {
        VIEW, EDIT, MANAGE
    }
}
