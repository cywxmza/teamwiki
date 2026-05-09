package com.teamwiki.repository;

import com.teamwiki.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Boolean existsByName(String name);
}
