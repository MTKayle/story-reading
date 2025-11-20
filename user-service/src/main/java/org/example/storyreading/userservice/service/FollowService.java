package org.example.storyreading.userservice.service;

import java.util.List;

public interface FollowService {
    boolean followStory(Long userId, Long storyId);
    boolean unfollowStory(Long userId, Long storyId);
    boolean isFollowing(Long userId, Long storyId);
    List<Long> getFollowedStoryIds(Long userId);
}

