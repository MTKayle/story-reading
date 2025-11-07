package org.example.storyreading.storyservice.service.impl;

import org.example.storyreading.storyservice.dto.StoryDtos;
import org.example.storyreading.storyservice.entity.PurchaseEntity;
import org.example.storyreading.storyservice.entity.StoryEntity;
import org.example.storyreading.storyservice.repository.PurchaseRepository;
import org.example.storyreading.storyservice.repository.StoryRepository;
import org.example.storyreading.storyservice.service.IPurchaseService;
import org.springframework.stereotype.Service;

@Service
public class PurchaseService implements IPurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final StoryRepository storyRepository;

    public PurchaseService(PurchaseRepository purchaseRepository, StoryRepository storyRepository) {
        this.purchaseRepository = purchaseRepository;
        this.storyRepository = storyRepository;
    }

    @Override
    public StoryDtos.PurchaseResponse purchaseStory(Long userId, Long storyId) {
        StoryEntity story = storyRepository.findById(storyId).orElseThrow(() -> new IllegalArgumentException("Story not found"));
        if (!story.isPaid()) {
            throw new IllegalArgumentException("Story is free. No purchase required");
        }
        if (purchaseRepository.existsByUserIdAndStory(userId, story)) {
            StoryDtos.PurchaseResponse resp = new StoryDtos.PurchaseResponse();
            resp.storyId = storyId;
            resp.userId = userId;
            resp.purchased = true;
            return resp;
        }
        PurchaseEntity p = new PurchaseEntity();
        p.setUserId(userId);
        p.setStory(story);
        purchaseRepository.save(p);

        StoryDtos.PurchaseResponse resp = new StoryDtos.PurchaseResponse();
        resp.storyId = storyId;
        resp.userId = userId;
        resp.purchased = true;
        return resp;
    }

    @Override
    public boolean hasPurchased(Long userId, Long storyId) {
        StoryEntity story = storyRepository.findById(storyId).orElseThrow(() -> new IllegalArgumentException("Story not found"));
        return purchaseRepository.existsByUserIdAndStory(userId, story);
    }
}


