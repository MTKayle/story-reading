package org.example.storyreading.userservice.repository;

import org.example.storyreading.userservice.entity.FollowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<FollowEntity, Long> {
    Optional<FollowEntity> findByUserIdAndStoryId(Long userId, Long storyId);
    boolean existsByUserIdAndStoryId(Long userId, Long storyId);
    List<FollowEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<FollowEntity> findByStoryId(Long storyId);
    void deleteByUserIdAndStoryId(Long userId, Long storyId);
}

