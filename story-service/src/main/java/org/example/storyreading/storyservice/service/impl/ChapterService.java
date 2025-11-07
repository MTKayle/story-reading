package org.example.storyreading.storyservice.service.impl;

import org.example.storyreading.storyservice.dto.StoryDtos;
import org.example.storyreading.storyservice.entity.ChapterEntity;
import org.example.storyreading.storyservice.entity.StoryEntity;
import org.example.storyreading.storyservice.repository.ChapterRepository;
import org.example.storyreading.storyservice.repository.StoryRepository;
import org.example.storyreading.storyservice.repository.PurchaseRepository;
import org.example.storyreading.storyservice.service.IChapterService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChapterService implements IChapterService {

    private final ChapterRepository chapterRepository;
    private final StoryRepository storyRepository;
    private final PurchaseRepository purchaseRepository;

    public ChapterService(ChapterRepository chapterRepository, StoryRepository storyRepository, PurchaseRepository purchaseRepository) {
        this.chapterRepository = chapterRepository;
        this.storyRepository = storyRepository;
        this.purchaseRepository = purchaseRepository;
    }

    @Override
    public StoryDtos.ChapterResponse createChapter(Long storyId, StoryDtos.CreateChapterRequest request) {
        StoryEntity story = storyRepository.findById(storyId).orElseThrow(() -> new IllegalArgumentException("Story not found"));

        // Service-level uniqueness check: ensure there is no existing chapter with the same number for this story
        Optional<ChapterEntity> existing = chapterRepository.findByStoryAndChapterNumber(story, request.chapterNumber);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Chapter number " + request.chapterNumber + " already exists for story id " + storyId);
        }

        ChapterEntity c = new ChapterEntity();
        c.setStory(story);
        c.setChapterNumber(request.chapterNumber);
        c.setTitle(request.title);
        c.setImageIds(request.imageIds == null ? null : String.join(",", request.imageIds));
        c = chapterRepository.save(c);
        return toDto(c);
    }

    @Override
    public List<StoryDtos.ChapterResponse> listChapters(Long storyId) {
        StoryEntity story = storyRepository.findById(storyId).orElseThrow(() -> new IllegalArgumentException("Story not found"));
        return chapterRepository.findByStoryOrderByChapterNumberAsc(story)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public StoryDtos.ChapterResponse getChapterForUser(Long userId, Long chapterId) {
        ChapterEntity c = chapterRepository.findById(chapterId).orElseThrow(() -> new IllegalArgumentException("Chapter not found"));
        StoryEntity story = c.getStory();
        if (story.isPaid()) {
            boolean purchased = purchaseRepository.existsByUserIdAndStory(userId, story);
            if (!purchased) {
                throw new IllegalArgumentException("You must purchase this story to read chapters");
            }
        }
        return toDto(c);
    }

    private StoryDtos.ChapterResponse toDto(ChapterEntity c) {
        StoryDtos.ChapterResponse dto = new StoryDtos.ChapterResponse();
        dto.id = c.getId();
        dto.storyId = c.getStory().getId();
        dto.chapterNumber = c.getChapterNumber();
        dto.title = c.getTitle();
        dto.imageIds = c.getImageIds() == null || c.getImageIds().isEmpty() ? null : Arrays.asList(c.getImageIds().split(","));
        return dto;
    }
}
