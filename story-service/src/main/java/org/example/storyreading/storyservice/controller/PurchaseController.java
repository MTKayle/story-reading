package org.example.storyreading.storyservice.controller;

import org.example.storyreading.storyservice.dto.StoryDtos;
import org.example.storyreading.storyservice.service.IPurchaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/story")
public class PurchaseController {

    private final IPurchaseService purchaseService;

    public PurchaseController(IPurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping("/{storyId}/purchase")
    public ResponseEntity<StoryDtos.PurchaseResponse> purchase(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable Long storyId,
            @RequestBody(required = false) StoryDtos.PurchaseRequest request) {
        return ResponseEntity.ok(purchaseService.purchaseStory(userId, storyId));
    }
}


