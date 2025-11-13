package org.example.storyreading.storyservice.service;

import org.example.storyreading.storyservice.dto.StoryDtos;

import java.util.List;

public interface IChapterService {
    StoryDtos.ChapterResponse createChapter(Long storyId, StoryDtos.CreateChapterRequest request);
    List<StoryDtos.ChapterResponse> listChapters(Long storyId);
    StoryDtos.ChapterResponse getChapterForUser(Long chapterId, Long userId);
    StoryDtos.ChapterResponse updateChapter(Long storyId, Long chapterId, StoryDtos.CreateChapterRequest request);
    void deleteChapter(Long storyId, Long chapterId);
}
