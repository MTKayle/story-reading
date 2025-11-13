package org.example.storyreading.storyservice.repository;

import org.example.storyreading.storyservice.entity.ChapterEntity;
import org.example.storyreading.storyservice.entity.StoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends JpaRepository<ChapterEntity, Long> {
    List<ChapterEntity> findByStoryOrderByChapterNumberAsc(StoryEntity story);

    // Finder used when uploading images for a specific chapter number
    Optional<ChapterEntity> findByStoryAndChapterNumber(StoryEntity story, int chapterNumber);
}
