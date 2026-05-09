package com.teamwiki.repository;

import com.teamwiki.entity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Page<Activity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT a FROM Activity a WHERE a.type = :type ORDER BY a.createdAt DESC")
    Page<Activity> findByTypeOrderByCreatedAtDesc(@Param("type") Activity.ActivityType type, Pageable pageable);

    void deleteByDocumentId(Long documentId);
}
