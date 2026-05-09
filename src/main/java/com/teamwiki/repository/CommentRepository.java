package com.teamwiki.repository;

import com.teamwiki.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByDocumentIdAndParentIdIsNull(Long documentId, Pageable pageable);
    Long countByDocumentId(Long documentId);
    void deleteByDocumentId(Long documentId);

    @Modifying
    @Query("UPDATE Comment c SET c.parent = null WHERE c.document.id = :documentId")
    void clearParentReferencesByDocumentId(@Param("documentId") Long documentId);
}
