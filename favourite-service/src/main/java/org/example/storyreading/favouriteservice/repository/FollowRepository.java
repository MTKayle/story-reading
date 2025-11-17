package org.example.storyreading.favouriteservice.repository;

import org.example.storyreading.favouriteservice.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByUserIdAndStoryId(Long userId, Long storyId);

    List<Follow> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Follow> findByStoryId(Long storyId);

    boolean existsByUserIdAndStoryId(Long userId, Long storyId);

    void deleteByUserIdAndStoryId(Long userId, Long storyId);

    long countByStoryId(Long storyId);
}

