package org.example.storyreading.storyservice.repository;

import org.example.storyreading.storyservice.entity.PurchaseEntity;
import org.example.storyreading.storyservice.entity.StoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<PurchaseEntity, Long> {
    boolean existsByUserIdAndStory(Long userId, StoryEntity story);
    Optional<PurchaseEntity> findByUserIdAndStory(Long userId, StoryEntity story);
}


