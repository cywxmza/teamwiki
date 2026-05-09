package com.teamwiki.repository;

import com.teamwiki.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserId(Long userId);
    Optional<Favorite> findByUserIdAndDocumentId(Long userId, Long documentId);
    Boolean existsByUserIdAndDocumentId(Long userId, Long documentId);
    void deleteByUserIdAndDocumentId(Long userId, Long documentId);
    void deleteByDocumentId(Long documentId);
}
