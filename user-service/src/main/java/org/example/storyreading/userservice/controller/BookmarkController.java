package org.example.storyreading.userservice.controller;

import org.example.storyreading.userservice.entity.BookmarkEntity;
import org.example.storyreading.userservice.security.JwtUtils;
import org.example.storyreading.userservice.service.BookmarkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/bookmarks")
public class BookmarkController {

    private static final Logger log = LoggerFactory.getLogger(BookmarkController.class);
    private final BookmarkService bookmarkService;
    private final JwtUtils jwtUtils;

    public BookmarkController(BookmarkService bookmarkService, JwtUtils jwtUtils) {
        this.bookmarkService = bookmarkService;
        this.jwtUtils = jwtUtils;
    }

    private Long extractUserIdFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            return null;
        }
        return jwtUtils.extractUserId(token);
    }

    @PostMapping
    public ResponseEntity<?> addBookmark(
            @RequestBody Map<String, Long> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("📖 Add bookmark request - storyId: {}, chapterId: {}, authHeader present: {}", 
                request.get("storyId"), request.get("chapterId"), authHeader != null);
        
        Long userId = extractUserIdFromHeader(authHeader);
        if (userId == null) {
            log.warn("❌ Unauthorized - userId is null");
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Long storyId = request.get("storyId");
        Long chapterId = request.get("chapterId");

        if (storyId == null || chapterId == null) {
            return ResponseEntity.status(400).body(Map.of("error", "storyId and chapterId are required"));
        }

        log.info("✅ User authenticated - userId: {}, storyId: {}, chapterId: {}", userId, storyId, chapterId);

        try {
            boolean success = bookmarkService.addBookmark(userId, storyId, chapterId);
            if (success) {
                log.info("✅ Successfully bookmarked - userId: {}, storyId: {}, chapterId: {}", userId, storyId, chapterId);
                return ResponseEntity.ok(Map.of("message", "Đã thêm bookmark", "bookmarked", true));
            } else {
                log.info("ℹ️ Already bookmarked - userId: {}, storyId: {}, chapterId: {}", userId, storyId, chapterId);
                return ResponseEntity.ok(Map.of("message", "Đã bookmark từ trước", "bookmarked", true));
            }
        } catch (Exception e) {
            log.error("❌ Error adding bookmark - userId: {}, storyId: {}, chapterId: {}", userId, storyId, chapterId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Internal server error"));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> removeBookmark(
            @RequestBody Map<String, Long> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = extractUserIdFromHeader(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Long storyId = request.get("storyId");
        Long chapterId = request.get("chapterId");

        if (storyId == null || chapterId == null) {
            return ResponseEntity.status(400).body(Map.of("error", "storyId and chapterId are required"));
        }

        try {
            boolean success = bookmarkService.removeBookmark(userId, storyId, chapterId);
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Đã xóa bookmark", "bookmarked", false));
            } else {
                return ResponseEntity.ok(Map.of("message", "Chưa bookmark chương này", "bookmarked", false));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/check")
    public ResponseEntity<?> checkBookmark(
            @RequestParam Long storyId,
            @RequestParam Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = extractUserIdFromHeader(authHeader);
        if (userId == null) {
            return ResponseEntity.ok(Map.of("bookmarked", false));
        }

        boolean isBookmarked = bookmarkService.isBookmarked(userId, storyId, chapterId);
        return ResponseEntity.ok(Map.of("bookmarked", isBookmarked));
    }

    @GetMapping
    public ResponseEntity<?> getUserBookmarks(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = extractUserIdFromHeader(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            List<BookmarkEntity> bookmarks = bookmarkService.getUserBookmarks(userId);
            List<Map<String, Object>> bookmarkDtos = bookmarks.stream()
                    .map(b -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("id", b.getId());
                        dto.put("storyId", b.getStoryId());
                        dto.put("chapterId", b.getChapterId());
                        dto.put("createdAt", b.getCreatedAt().toString());
                        return dto;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("bookmarks", bookmarkDtos));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/story/{storyId}")
    public ResponseEntity<?> getStoryBookmarks(
            @PathVariable Long storyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = extractUserIdFromHeader(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            List<BookmarkEntity> bookmarks = bookmarkService.getStoryBookmarks(userId, storyId);
            List<Map<String, Object>> bookmarkDtos = bookmarks.stream()
                    .map(b -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("id", b.getId());
                        dto.put("storyId", b.getStoryId());
                        dto.put("chapterId", b.getChapterId());
                        dto.put("createdAt", b.getCreatedAt().toString());
                        return dto;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("bookmarks", bookmarkDtos));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}

