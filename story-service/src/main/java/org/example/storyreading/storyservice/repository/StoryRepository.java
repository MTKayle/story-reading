package org.example.storyreading.storyservice.repository;

import org.example.storyreading.storyservice.entity.StoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StoryRepository extends JpaRepository<StoryEntity, Long> {
    // Find by title (partial, case-insensitive)
    List<StoryEntity> findByTitleContainingIgnoreCase(String title);

    // Find by genre contained in the comma-separated genres column (case-insensitive) with pagination
    Page<StoryEntity> findByGenresContainingIgnoreCase(String genre, Pageable pageable);

    // Atomically increment view count for a story (clearAutomatically so persistence context sees changes)
    @Modifying(clearAutomatically = true)
    @Query("update StoryEntity s set s.viewCount = s.viewCount + 1 where s.id = :id")
    int incrementViewCountById(@Param("id") Long id);
}
