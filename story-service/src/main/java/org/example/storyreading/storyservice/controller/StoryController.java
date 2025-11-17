package org.example.storyreading.storyservice.controller;

import org.example.storyreading.storyservice.dto.StoryDtos;
import org.example.storyreading.storyservice.service.IStoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/story")
public class StoryController {

    private final IStoryService storyService;

    public StoryController(IStoryService storyService) {
        this.storyService = storyService;
    }

    @PostMapping
    public ResponseEntity<StoryDtos.StoryResponse> createStory(
            @RequestHeader(value = "X-User-Id") Long userId,
            @RequestBody StoryDtos.CreateStoryRequest request) {
        return ResponseEntity.ok(storyService.createStory(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoryDtos.StoryResponse> getStory(@PathVariable Long id) {
        return ResponseEntity.ok(storyService.getStory(id));
    }

    @GetMapping
    public ResponseEntity<List<StoryDtos.StoryResponse>> listStories() {
        return ResponseEntity.ok(storyService.listStories());
    }

    @PutMapping("/{id}")
    public ResponseEntity<StoryDtos.StoryResponse> updateStory(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable Long id,
            @RequestBody StoryDtos.UpdateStoryRequest request) {
        return ResponseEntity.ok(storyService.updateStory(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStory(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable Long id) {
        storyService.deleteStory(userId, id);
        return ResponseEntity.noContent().build();
    }

    // Search stories by title (partial, case-insensitive)
    @GetMapping("/search")
    public ResponseEntity<List<StoryDtos.StoryResponse>> searchByTitle(@RequestParam("title") String title) {
        return ResponseEntity.ok(storyService.searchByTitle(title));
    }

    // Get stories by genre with pagination (page starts at 0)
    @GetMapping("/genre/{genre}")
    public ResponseEntity<List<StoryDtos.StoryResponse>> getByGenre(
            @PathVariable String genre,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        // Path variables may contain percent-encoding or plus signs; decode to produce the intended genre string.
        String decoded = URLDecoder.decode(genre, StandardCharsets.UTF_8);
        // In some clients '+' is used to represent space in query encoding; ensure '+' becomes space.
        if (decoded.indexOf('+') >= 0) {
            decoded = decoded.replace('+', ' ');
        }
        return ResponseEntity.ok(storyService.getStoriesByGenre(decoded, page, size));
    }
}
