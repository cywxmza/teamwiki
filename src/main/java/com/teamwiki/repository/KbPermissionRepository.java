package com.teamwiki.repository;

import com.teamwiki.entity.KbPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KbPermissionRepository extends JpaRepository<KbPermission, Long> {

    List<KbPermission> findByKnowledgeBaseId(Long kbId);

    boolean existsByKnowledgeBaseIdAndUserId(Long kbId, Long userId);

    void deleteByKnowledgeBaseIdAndUserId(Long kbId, Long userId);

    List<KbPermission> findByUserId(Long userId);
}
