package com.teamwiki.repository;

import com.teamwiki.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    List<DocumentVersion> findByDocumentIdOrderByVersionNumberDesc(Long documentId);
    Optional<DocumentVersion> findTopByDocumentIdOrderByVersionNumberDesc(Long documentId);
    void deleteByDocumentId(Long documentId);
}
