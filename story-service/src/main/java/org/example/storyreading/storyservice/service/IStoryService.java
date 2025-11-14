package org.example.storyreading.storyservice.service;

import org.example.storyreading.storyservice.dto.StoryDtos;

import java.util.List;

public interface IStoryService {
    StoryDtos.StoryResponse createStory(Long authorId, StoryDtos.CreateStoryRequest request);
    StoryDtos.StoryResponse getStory(Long id);
    List<StoryDtos.StoryResponse> listStories();
    StoryDtos.StoryResponse updateStory(Long authorId, Long storyId, StoryDtos.UpdateStoryRequest request);
    void deleteStory(Long authorId, Long storyId);

    // Search stories by title (partial, case-insensitive)
    List<StoryDtos.StoryResponse> searchByTitle(String title);

    // Get stories by genre with pagination
    List<StoryDtos.StoryResponse> getStoriesByGenre(String genre, int page, int size);
}
