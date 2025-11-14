package org.example.storyreading.storyservice.controller;

import org.example.storyreading.storyservice.dto.StoryDtos;
import org.example.storyreading.storyservice.service.IChapterService;
import org.example.storyreading.storyservice.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/story")
public class ChapterController {

    private final IChapterService chapterService;
    private final JwtUtil jwtUtil;

    public ChapterController(IChapterService chapterService, JwtUtil jwtUtil) {
        this.chapterService = chapterService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/{storyId}/chapters")
    public ResponseEntity<StoryDtos.ChapterResponse> createChapter(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable Long storyId,
            @RequestBody StoryDtos.CreateChapterRequest request) {
        return ResponseEntity.ok(chapterService.createChapter(storyId, request));
    }

    @GetMapping("/{storyId}/chapters")
    public ResponseEntity<List<StoryDtos.ChapterResponse>> listChapters(@PathVariable Long storyId) {
        return ResponseEntity.ok(chapterService.listChapters(storyId));
    }

    @GetMapping("/chapters/{chapterId}")
    public ResponseEntity<StoryDtos.ChapterResponse> getChapter(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @PathVariable Long chapterId) {

        // Ưu tiên: 1) X-User-Id header, 2) JWT token, 3) null
        Long userId = headerUserId;
        if (userId == null && authorizationHeader != null) {
            userId = jwtUtil.extractUserIdFromHeader(authorizationHeader);
        }

        return ResponseEntity.ok(chapterService.getChapterForUser(chapterId, userId));
    }

    @PutMapping("/{storyId}/chapters/{chapterId}")
    public ResponseEntity<StoryDtos.ChapterResponse> updateChapter(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable Long storyId,
            @PathVariable Long chapterId,
            @RequestBody StoryDtos.CreateChapterRequest request) {
        return ResponseEntity.ok(chapterService.updateChapter(storyId, chapterId, request));
    }

    @DeleteMapping("/{storyId}/chapters/{chapterId}")
    public ResponseEntity<Void> deleteChapter(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable Long storyId,
            @PathVariable Long chapterId) {
        chapterService.deleteChapter(storyId, chapterId);
        return ResponseEntity.noContent().build();
    }
}
