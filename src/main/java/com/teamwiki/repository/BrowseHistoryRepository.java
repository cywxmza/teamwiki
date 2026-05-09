package com.teamwiki.repository;

import com.teamwiki.entity.BrowseHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrowseHistoryRepository extends JpaRepository<BrowseHistory, Long> {
    Page<BrowseHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    void deleteByUserId(Long userId);
    void deleteByDocumentId(Long documentId);
}
