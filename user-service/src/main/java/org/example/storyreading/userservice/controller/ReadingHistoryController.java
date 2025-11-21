package org.example.storyreading.userservice.controller;

import org.example.storyreading.userservice.entity.ReadingHistoryEntity;
import org.example.storyreading.userservice.security.JwtUtils;
import org.example.storyreading.userservice.service.ReadingHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/history")
public class ReadingHistoryController {

    private static final Logger log = LoggerFactory.getLogger(ReadingHistoryController.class);
    private final ReadingHistoryService readingHistoryService;
    private final JwtUtils jwtUtils;

    public ReadingHistoryController(ReadingHistoryService readingHistoryService, JwtUtils jwtUtils) {
        this.readingHistoryService = readingHistoryService;
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

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> getUserHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("📖 Get user history request - authHeader present: {}", authHeader != null);
        
        Long userId = extractUserIdFromHeader(authHeader);
        if (userId == null) {
            log.warn("❌ Unauthorized - userId is null");
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        log.info("✅ User authenticated - userId: {}", userId);

        try {
            List<ReadingHistoryEntity> histories = readingHistoryService.getUserHistory(userId);
            List<Map<String, Object>> historyDtos = histories.stream()
                    .map(h -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("id", h.getId());
                        dto.put("storyId", h.getStoryId());
                        dto.put("chapterId", h.getChapterId());
                        dto.put("lastReadAt", h.getLastReadAt().toString());
                        return dto;
                    })
                    .collect(Collectors.toList());
            
            log.info("✅ Successfully retrieved {} history items for userId: {}", historyDtos.size(), userId);
            return ResponseEntity.ok(Map.of("history", historyDtos));
        } catch (Exception e) {
            log.error("❌ Error retrieving history - userId: {}", userId, e);
            e.printStackTrace(); // Print stack trace for debugging
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Internal server error"));
        }
    }

    @PostMapping
    public ResponseEntity<?> saveHistory(
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
            readingHistoryService.saveOrUpdateHistory(userId, storyId, chapterId);
            return ResponseEntity.ok(Map.of("message", "Đã lưu lịch sử đọc"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{storyId}")
    public ResponseEntity<?> deleteHistory(
            @PathVariable Long storyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = extractUserIdFromHeader(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            readingHistoryService.deleteHistory(userId, storyId);
            return ResponseEntity.ok(Map.of("message", "Đã xóa lịch sử đọc"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAllHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = extractUserIdFromHeader(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            readingHistoryService.deleteAllHistory(userId);
            return ResponseEntity.ok(Map.of("message", "Đã xóa toàn bộ lịch sử đọc"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}

