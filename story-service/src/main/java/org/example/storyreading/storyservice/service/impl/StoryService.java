package org.example.storyreading.storyservice.service.impl;

import org.example.storyreading.storyservice.dto.StoryDtos;
import org.example.storyreading.storyservice.entity.StoryEntity;
import org.example.storyreading.storyservice.repository.StoryRepository;
import org.example.storyreading.storyservice.service.IStoryService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StoryService implements IStoryService {

    private final StoryRepository storyRepository;

    public StoryService(StoryRepository storyRepository) {
        this.storyRepository = storyRepository;
    }

    @Override
    public StoryDtos.StoryResponse createStory(Long userId, StoryDtos.CreateStoryRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        StoryEntity s = new StoryEntity();
        s.setAuthor(String.valueOf(userId)); // Set author from userId header
        s.setTitle(request.title);
        s.setDescription(request.description);
        s.setGenres(request.genres == null ? null : String.join(",", request.genres));
        s.setCoverImageId(request.coverImageId);
        s.setPaid(request.paid);
        s.setPrice(request.price);
        s = storyRepository.save(s);
        return toDto(s);
    }

    @Override
    public StoryDtos.StoryResponse getStory(Long id) {
        StoryEntity s = storyRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Story not found"));
        return toDto(s);
    }

    @Override
    public List<StoryDtos.StoryResponse> listStories() {
        return storyRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    private StoryDtos.StoryResponse toDto(StoryEntity s) {
        StoryDtos.StoryResponse dto = new StoryDtos.StoryResponse();
        dto.id = s.getId();
        dto.title = s.getTitle();
        dto.description = s.getDescription();
        dto.genres = s.getGenres() == null || s.getGenres().isEmpty() ? null : Arrays.asList(s.getGenres().split(","));
        dto.coverImageId = s.getCoverImageId();
        dto.paid = s.isPaid();
        dto.price = s.getPrice();
        dto.author = s.getAuthor();
        return dto;
    }
}
