package org.example.storyreading.userservice.service;

import org.example.storyreading.userservice.entity.ReadingHistoryEntity;

import java.util.List;

public interface ReadingHistoryService {
    void saveOrUpdateHistory(Long userId, Long storyId, Long chapterId);
    List<ReadingHistoryEntity> getUserHistory(Long userId);
    void deleteHistory(Long userId, Long storyId);
    void deleteAllHistory(Long userId);
}

