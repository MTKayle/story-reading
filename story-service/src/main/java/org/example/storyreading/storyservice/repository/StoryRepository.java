package org.example.storyreading.storyservice.repository;

import org.example.storyreading.storyservice.entity.StoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryRepository extends JpaRepository<StoryEntity, Long> {
}


