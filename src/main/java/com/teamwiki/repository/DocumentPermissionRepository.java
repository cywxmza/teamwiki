package com.teamwiki.repository;

import com.teamwiki.entity.DocumentPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentPermissionRepository extends JpaRepository<DocumentPermission, Long> {
    List<DocumentPermission> findByDocumentId(Long documentId);
    List<DocumentPermission> findByUserId(Long userId);
    Optional<DocumentPermission> findByDocumentIdAndUserId(Long documentId, Long userId);
    void deleteByDocumentIdAndUserId(Long documentId, Long userId);
    void deleteByDocumentId(Long documentId);
}
