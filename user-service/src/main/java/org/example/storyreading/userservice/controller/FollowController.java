package org.example.storyreading.userservice.controller;

import org.example.storyreading.userservice.security.JwtUtils;
import org.example.storyreading.userservice.service.FollowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/follow")
public class FollowController {

    private static final Logger log = LoggerFactory.getLogger(FollowController.class);
    private final FollowService followService;
    private final JwtUtils jwtUtils;

    public FollowController(FollowService followService, JwtUtils jwtUtils) {
        this.followService = followService;
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

    @PostMapping("/{storyId}")
    public ResponseEntity<?> followStory(
            @PathVariable Long storyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("📖 Follow story request - storyId: {}, authHeader present: {}", storyId, authHeader != null);
        
        Long userId = extractUserIdFromHeader(authHeader);
        if (userId == null) {
            log.warn("❌ Unauthorized - userId is null");
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        log.info("✅ User authenticated - userId: {}, storyId: {}", userId, storyId);

        try {
            boolean success = followService.followStory(userId, storyId);
            if (success) {
                log.info("✅ Successfully followed story - userId: {}, storyId: {}", userId, storyId);
                return ResponseEntity.ok(Map.of("message", "Đã theo dõi truyện", "following", true));
            } else {
                log.info("ℹ️ Already following - userId: {}, storyId: {}", userId, storyId);
                return ResponseEntity.ok(Map.of("message", "Đã theo dõi truyện từ trước", "following", true));
            }
        } catch (Exception e) {
            log.error("❌ Error following story - userId: {}, storyId: {}", userId, storyId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Internal server error"));
        }
    }

    @DeleteMapping("/{storyId}")
    public ResponseEntity<?> unfollowStory(
            @PathVariable Long storyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = extractUserIdFromHeader(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            boolean success = followService.unfollowStory(userId, storyId);
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Đã bỏ theo dõi truyện", "following", false));
            } else {
                return ResponseEntity.ok(Map.of("message", "Chưa theo dõi truyện này", "following", false));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{storyId}/check")
    public ResponseEntity<?> checkFollowing(
            @PathVariable Long storyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = extractUserIdFromHeader(authHeader);
        if (userId == null) {
            return ResponseEntity.ok(Map.of("following", false));
        }

        boolean isFollowing = followService.isFollowing(userId, storyId);
        return ResponseEntity.ok(Map.of("following", isFollowing));
    }

    @GetMapping("/list")
    public ResponseEntity<?> getFollowedStories(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = extractUserIdFromHeader(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            List<Long> storyIds = followService.getFollowedStoryIds(userId);
            return ResponseEntity.ok(Map.of("storyIds", storyIds));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}

