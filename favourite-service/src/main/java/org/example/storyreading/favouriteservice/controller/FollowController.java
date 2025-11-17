package org.example.storyreading.favouriteservice.controller;

import org.example.storyreading.favouriteservice.dto.FollowDto;
import org.example.storyreading.favouriteservice.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/follows")
public class FollowController {

    @Autowired
    private FollowService followService;

    /**
     * Theo dõi một truyện
     */
    @PostMapping
    public ResponseEntity<FollowDto.FollowResponse> followStory(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody FollowDto.FollowRequest request) {
        try {
            FollowDto.FollowResponse response = followService.followStory(userId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Bỏ theo dõi một truyện
     */
    @DeleteMapping("/story/{storyId}")
    public ResponseEntity<Void> unfollowStory(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long storyId) {
        try {
            followService.unfollowStory(userId, storyId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy danh sách các truyện user đang theo dõi
     */
    @GetMapping
    public ResponseEntity<List<FollowDto.FollowResponse>> getUserFollows(
            @RequestHeader("X-User-Id") Long userId) {
        List<FollowDto.FollowResponse> follows = followService.getUserFollows(userId);
        return ResponseEntity.ok(follows);
    }

    /**
     * Kiểm tra user có đang theo dõi truyện không
     */
    @GetMapping("/story/{storyId}/check")
    public ResponseEntity<Boolean> isFollowing(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long storyId) {
        boolean isFollowing = followService.isFollowing(userId, storyId);
        return ResponseEntity.ok(isFollowing);
    }

    /**
     * Lấy trạng thái follow và số lượng người theo dõi của một truyện
     */
    @GetMapping("/story/{storyId}/status")
    public ResponseEntity<FollowDto.FollowStatusResponse> getFollowStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long storyId) {
        FollowDto.FollowStatusResponse status = followService.getFollowStatus(userId, storyId);
        return ResponseEntity.ok(status);
    }

    /**
     * Lấy số lượng người theo dõi của một truyện (public API)
     */
    @GetMapping("/story/public/{storyId}/count")
    public ResponseEntity<Long> getFollowerCount(@PathVariable Long storyId) {
        long count = followService.getStoryFollowerCount(storyId);
        return ResponseEntity.ok(count);
    }

    /**
     * Lấy danh sách userId của người theo dõi truyện (internal API)
     */
    @GetMapping("/story/{storyId}/followers")
    public ResponseEntity<List<Long>> getStoryFollowers(@PathVariable Long storyId) {
        List<Long> followerIds = followService.getStoryFollowerIds(storyId);
        return ResponseEntity.ok(followerIds);
    }
}
