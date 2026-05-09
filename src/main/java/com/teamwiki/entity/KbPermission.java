package com.teamwiki.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "kb_permissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kb_id", nullable = false)
    private KnowledgeBase knowledgeBase;

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
