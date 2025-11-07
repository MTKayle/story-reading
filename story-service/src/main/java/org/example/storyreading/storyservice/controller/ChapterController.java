package org.example.storyreading.storyservice.controller;

import org.example.storyreading.storyservice.dto.StoryDtos;
import org.example.storyreading.storyservice.service.IChapterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/story")
public class ChapterController {

    private final IChapterService chapterService;

    public ChapterController(IChapterService chapterService) {
        this.chapterService = chapterService;
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
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable Long chapterId) {
        return ResponseEntity.ok(chapterService.getChapterForUser(userId, chapterId));
    }
}


