package com.teamwiki.repository;

import com.teamwiki.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a ORDER BY a.createdAt DESC")
    Page<AuditLog> findLatestLogs(Pageable pageable);

    @Query("SELECT COUNT(DISTINCT a.user.id) FROM AuditLog a WHERE a.createdAt >= :since")
    Long countActiveUsersSince(@Param("since") LocalDateTime since);
}
