package com.teamwiki.repository;

import com.teamwiki.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByParentIdIsNullOrderBySortOrderAsc();
    List<Category> findByParentIdOrderBySortOrderAsc(Long parentId);
    Long countByParentId(Long parentId);
}
