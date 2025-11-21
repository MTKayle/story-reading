package org.example.storyreading.userservice.repository;

import org.example.storyreading.userservice.entity.ReadingHistoryEntity;
import org.example.storyreading.userservice.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingHistoryRepository extends JpaRepository<ReadingHistoryEntity, Long> {
    Optional<ReadingHistoryEntity> findByUserAndStoryId(UserEntity user, Long storyId);
    boolean existsByUserAndStoryId(UserEntity user, Long storyId);
    List<ReadingHistoryEntity> findByUserOrderByLastReadAtDesc(UserEntity user);
    void deleteByUserAndStoryId(UserEntity user, Long storyId);
    void deleteByUser(UserEntity user);
}

