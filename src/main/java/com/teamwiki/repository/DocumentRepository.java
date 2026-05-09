package com.teamwiki.repository;

import com.teamwiki.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    Page<Document> findByIsDeletedFalse(Pageable pageable);

    Page<Document> findByIsDeletedTrue(Pageable pageable);

    Page<Document> findByCreatedByIdAndIsDeletedFalse(Long userId, Pageable pageable);

    Page<Document> findByKnowledgeBaseIdAndIsDeletedFalse(Long kbId, Pageable pageable);

    @Query("SELECT d FROM Document d JOIN d.tags t WHERE t.id = :tagId AND d.isDeleted = false")
    Page<Document> findByTagIdAndIsDeletedFalse(@Param("tagId") Long tagId, Pageable pageable);

    Page<Document> findByCategoryIdAndIsDeletedFalse(Long categoryId, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.isDeleted = false AND (LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(d.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Document> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT d FROM Document d JOIN DocumentPermission p ON d.id = p.document.id WHERE p.user.id = :userId AND p.permissionLevel = 'EDIT' AND d.isDeleted = false")
    Page<Document> findParticipatedDocuments(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.isDeleted = false ORDER BY d.viewCount DESC")
    List<Document> findTopByViewCount(Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.isDeleted = false AND d.createdBy.id = :userId ORDER BY d.updatedAt DESC")
    List<Document> findRecentByUser(@Param("userId") Long userId, Pageable pageable);

    Optional<Document> findByShareToken(String shareToken);
}
