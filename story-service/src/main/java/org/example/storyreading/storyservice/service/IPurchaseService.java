package org.example.storyreading.storyservice.service;

import org.example.storyreading.storyservice.dto.StoryDtos;

public interface IPurchaseService {
    StoryDtos.PurchaseResponse purchaseStory(Long userId, Long storyId);
    boolean hasPurchased(Long userId, Long storyId);
    void grantAccess(Long userId, Long storyId);
}
