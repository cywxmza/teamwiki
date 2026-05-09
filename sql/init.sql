-- ============================================================
-- 智识库（TeamWiki）企业知识库协同平台 - 数据库初始化脚本
-- 数据库：MySQL 8.0+
-- 字符集：utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS `teamwiki` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `teamwiki`;

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE `users` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `username`   VARCHAR(100) NOT NULL,
  `email`      VARCHAR(150) NOT NULL,
  `password`   VARCHAR(255) NOT NULL,
  `nickname`   VARCHAR(50)  DEFAULT NULL,
  `avatar`     VARCHAR(500) DEFAULT NULL,
  `department` VARCHAR(50)  DEFAULT NULL,
  `role`       ENUM('ADMIN','USER') NOT NULL,
  `enabled`    BIT(1)       DEFAULT 1,
  `created_at` DATETIME(6)  DEFAULT NULL,
  `updated_at` DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`),
  UNIQUE KEY `uk_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. 分类表（支持树形结构）
-- ============================================================
CREATE TABLE `categories` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(100) NOT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `icon`        VARCHAR(20)  DEFAULT NULL,
  `sort_order`  INT          DEFAULT 0,
  `parent_id`   BIGINT       DEFAULT NULL,
  `created_at`  DATETIME(6)  DEFAULT NULL,
  `updated_at`  DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_categories_parent` (`parent_id`),
  CONSTRAINT `fk_categories_parent` FOREIGN KEY (`parent_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 3. 标签表
-- ============================================================
CREATE TABLE `tags` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT,
  `name`       VARCHAR(50) NOT NULL,
  `color`      VARCHAR(20) DEFAULT NULL,
  `created_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tags_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 4. 文档表
-- ============================================================
CREATE TABLE `documents` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `title`          VARCHAR(255) NOT NULL,
  `content`        TEXT         DEFAULT NULL,
  `summary`        TEXT         DEFAULT NULL,
  `visibility`     ENUM('PRIVATE','TEAM','PUBLIC') NOT NULL DEFAULT 'PRIVATE',
  `allow_comment`  BIT(1)       DEFAULT 1,
  `is_deleted`     BIT(1)       DEFAULT 0,
  `view_count`     BIGINT       DEFAULT 0,
  `deleted_at`     DATETIME(6)  DEFAULT NULL,
  `published_at`   DATETIME(6)  DEFAULT NULL,
  `category_id`    BIGINT       DEFAULT NULL,
  `created_by`     BIGINT       NOT NULL,
  `last_edited_by` BIGINT       DEFAULT NULL,
  `created_at`     DATETIME(6)  DEFAULT NULL,
  `updated_at`     DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_documents_category` (`category_id`),
  KEY `idx_documents_created_by` (`created_by`),
  KEY `idx_documents_last_edited_by` (`last_edited_by`),
  KEY `idx_documents_is_deleted` (`is_deleted`),
  CONSTRAINT `fk_documents_category`    FOREIGN KEY (`category_id`)    REFERENCES `categories` (`id`),
  CONSTRAINT `fk_documents_created_by`  FOREIGN KEY (`created_by`)     REFERENCES `users` (`id`),
  CONSTRAINT `fk_documents_last_edited` FOREIGN KEY (`last_edited_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 5. 文档-标签关联表
-- ============================================================
CREATE TABLE `document_tags` (
  `document_id` BIGINT NOT NULL,
  `tag_id`      BIGINT NOT NULL,
  KEY `idx_document_tags_tag` (`tag_id`),
  KEY `idx_document_tags_doc` (`document_id`),
  CONSTRAINT `fk_document_tags_tag` FOREIGN KEY (`tag_id`)      REFERENCES `tags` (`id`),
  CONSTRAINT `fk_document_tags_doc` FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 6. 文档权限表
-- ============================================================
CREATE TABLE `document_permissions` (
  `id`               BIGINT NOT NULL AUTO_INCREMENT,
  `permission_level`  ENUM('VIEW','EDIT','MANAGE') NOT NULL,
  `document_id`      BIGINT NOT NULL,
  `user_id`          BIGINT NOT NULL,
  `created_at`       DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_doc_permissions_document` (`document_id`),
  KEY `idx_doc_permissions_user`     (`user_id`),
  CONSTRAINT `fk_doc_permissions_document` FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`),
  CONSTRAINT `fk_doc_permissions_user`     FOREIGN KEY (`user_id`)     REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 7. 文档版本表
-- ============================================================
CREATE TABLE `document_versions` (
  `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
  `version_number`     INT          NOT NULL,
  `content_snapshot`   TEXT         DEFAULT NULL,
  `change_description` VARCHAR(255) DEFAULT NULL,
  `document_id`        BIGINT       NOT NULL,
  `created_by`         BIGINT       NOT NULL,
  `created_at`         DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_doc_versions_document` (`document_id`),
  KEY `idx_doc_versions_created`  (`created_by`),
  CONSTRAINT `fk_doc_versions_document` FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`),
  CONSTRAINT `fk_doc_versions_created`  FOREIGN KEY (`created_by`)  REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 8. 评论表（支持嵌套回复）
-- ============================================================
CREATE TABLE `comments` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `content`       TEXT         NOT NULL,
  `selected_text` VARCHAR(500) DEFAULT NULL,
  `is_inline`     BIT(1)       DEFAULT 0,
  `is_resolved`   BIT(1)       DEFAULT 0,
  `document_id`   BIGINT       NOT NULL,
  `parent_id`     BIGINT       DEFAULT NULL,
  `user_id`       BIGINT       NOT NULL,
  `created_at`    DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_comments_document` (`document_id`),
  KEY `idx_comments_parent`   (`parent_id`),
  KEY `idx_comments_user`     (`user_id`),
  CONSTRAINT `fk_comments_document` FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`),
  CONSTRAINT `fk_comments_parent`   FOREIGN KEY (`parent_id`)   REFERENCES `comments` (`id`),
  CONSTRAINT `fk_comments_user`     FOREIGN KEY (`user_id`)     REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 9. 收藏表
-- ============================================================
CREATE TABLE `favorites` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT      NOT NULL,
  `document_id` BIGINT      NOT NULL,
  `created_at`  DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_favorites_user_doc` (`user_id`, `document_id`),
  KEY `idx_favorites_document` (`document_id`),
  CONSTRAINT `fk_favorites_user`     FOREIGN KEY (`user_id`)     REFERENCES `users` (`id`),
  CONSTRAINT `fk_favorites_document` FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 10. 浏览历史表
-- ============================================================
CREATE TABLE `browse_history` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT      NOT NULL,
  `document_id` BIGINT      NOT NULL,
  `created_at`  DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_browse_history_user` (`user_id`),
  KEY `idx_browse_history_doc`  (`document_id`),
  CONSTRAINT `fk_browse_history_user` FOREIGN KEY (`user_id`)     REFERENCES `users` (`id`),
  CONSTRAINT `fk_browse_history_doc`  FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 11. 通知表
-- ============================================================
CREATE TABLE `notifications` (
  `id`           BIGINT NOT NULL AUTO_INCREMENT,
  `type`         ENUM('COLLABORATION_INVITE','DOCUMENT_EDITED','MENTION','COMMENT','PERMISSION_CHANGE','SYSTEM') NOT NULL,
  `message`      TEXT   NOT NULL,
  `payload`      TEXT   DEFAULT NULL,
  `is_read`      BIT(1) DEFAULT 0,
  `document_id`  BIGINT DEFAULT NULL,
  `user_id`      BIGINT NOT NULL,
  `created_at`   DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_notifications_user` (`user_id`),
  CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 12. 活动动态表
-- ============================================================
CREATE TABLE `activities` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `type`         ENUM('CREATE','EDIT','DELETE','RESTORE','COMMENT','PERMISSION_CHANGE','SHARE') NOT NULL,
  `description`  VARCHAR(500) DEFAULT NULL,
  `document_id`  BIGINT       DEFAULT NULL,
  `user_id`      BIGINT       NOT NULL,
  `created_at`   DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_activities_document` (`document_id`),
  KEY `idx_activities_user`     (`user_id`),
  CONSTRAINT `fk_activities_document` FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`),
  CONSTRAINT `fk_activities_user`     FOREIGN KEY (`user_id`)     REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 初始数据：默认管理员和演示用户
-- 密码使用BCrypt加密，明文分别为 admin123 / demo123
-- ============================================================
INSERT INTO `users` (`username`, `email`, `password`, `nickname`, `department`, `role`, `enabled`) VALUES
('admin', 'admin@teamwiki.com', '$2a$10$/pdKv3KkZARNWk91qhNxe.n6tlEJe.XCSFMIgNS7Mag0RU4xm5XSy', '系统管理员', '技术部', 'ADMIN', 1),
('demo',  'demo@teamwiki.com',  '$2a$10$1jgiCV6dv8LOhORrwr2zzOJwagaLVvJJRV6ztn/S4Hsma.2JiltGi', '演示用户',   '产品部', 'USER',  1);
