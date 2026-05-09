package com.teamwiki.repository;

import com.teamwiki.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    List<KnowledgeBase> findByIsDeletedFalse();

    @Query("SELECT kb FROM KnowledgeBase kb WHERE kb.isDeleted = false AND (kb.visibility = :visibility OR kb.owner.id = :userId OR kb.department = :department OR kb.id IN (SELECT p.knowledgeBase.id FROM KbPermission p WHERE p.user.id = :userId))")
    List<KnowledgeBase> findAccessible(@Param("userId") Long userId, @Param("department") String department, @Param("visibility") KnowledgeBase.KbVisibility visibility);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.knowledgeBase.id = :kbId AND d.isDeleted = false")
    Long countDocumentsByKbId(@Param("kbId") Long kbId);
}
