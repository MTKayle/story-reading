package org.example.storyreading.storyservice.repository;

import org.example.storyreading.storyservice.entity.StoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoryRepository extends JpaRepository<StoryEntity, Long> {
    // Find by title (partial, case-insensitive)
    List<StoryEntity> findByTitleContainingIgnoreCase(String title);

    // Find by genre contained in the comma-separated genres column (case-insensitive) with pagination
    Page<StoryEntity> findByGenresContainingIgnoreCase(String genre, Pageable pageable);

    // Find all stories containing a specific genre (case-insensitive)
    List<StoryEntity> findByGenresContainingIgnoreCase(String genre);

    long countByGenresContainingIgnoreCase(String genre);
}
