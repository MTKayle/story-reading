package org.example.storyreading.storyservice.service;

import org.example.storyreading.storyservice.dto.StoryDtos;

import java.util.List;

public interface IChapterService {
    StoryDtos.ChapterResponse createChapter(Long storyId, StoryDtos.CreateChapterRequest request);
    List<StoryDtos.ChapterResponse> listChapters(Long storyId);
    StoryDtos.ChapterResponse getChapterForUser(Long userId, Long chapterId);
}


