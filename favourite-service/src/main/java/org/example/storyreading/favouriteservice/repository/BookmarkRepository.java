package org.example.storyreading.favouriteservice.repository;

import org.example.storyreading.favouriteservice.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByUserIdAndStoryId(Long userId, Long storyId);

    List<Bookmark> findByUserIdOrderByLastReadAtDesc(Long userId);

    boolean existsByUserIdAndStoryId(Long userId, Long storyId);

    void deleteByUserIdAndStoryId(Long userId, Long storyId);
}

