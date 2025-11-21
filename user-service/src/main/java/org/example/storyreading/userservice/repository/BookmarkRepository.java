package org.example.storyreading.userservice.repository;

import org.example.storyreading.userservice.entity.BookmarkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<BookmarkEntity, Long> {
    Optional<BookmarkEntity> findByUserIdAndStoryIdAndChapterId(Long userId, Long storyId, Long chapterId);
    boolean existsByUserIdAndStoryIdAndChapterId(Long userId, Long storyId, Long chapterId);
    List<BookmarkEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    void deleteByUserIdAndStoryIdAndChapterId(Long userId, Long storyId, Long chapterId);
    List<BookmarkEntity> findByUserIdAndStoryId(Long userId, Long storyId);
}

