package org.example.storyreading.storyservice.service;

import org.example.storyreading.storyservice.dto.StoryDtos;

import java.util.List;

public interface IStoryService {
    StoryDtos.StoryResponse createStory(Long authorId, StoryDtos.CreateStoryRequest request);
    StoryDtos.StoryResponse getStory(Long id);
    List<StoryDtos.StoryResponse> listStories();
}


