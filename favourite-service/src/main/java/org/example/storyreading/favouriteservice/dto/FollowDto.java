package org.example.storyreading.favouriteservice.dto;

import java.time.LocalDateTime;

public class FollowDto {

    public static class FollowRequest {
        private Long storyId;

        public Long getStoryId() {
            return storyId;
        }

        public void setStoryId(Long storyId) {
            this.storyId = storyId;
        }
    }

    public static class FollowResponse {
        private Long id;
        private Long userId;
        private Long storyId;
        private LocalDateTime createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getStoryId() {
            return storyId;
        }

        public void setStoryId(Long storyId) {
            this.storyId = storyId;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class FollowStatusResponse {
        private boolean isFollowing;
        private long followerCount;

        public FollowStatusResponse(boolean isFollowing, long followerCount) {
            this.isFollowing = isFollowing;
            this.followerCount = followerCount;
        }

        public boolean isFollowing() {
            return isFollowing;
        }

        public void setFollowing(boolean following) {
            isFollowing = following;
        }

        public long getFollowerCount() {
            return followerCount;
        }

        public void setFollowerCount(long followerCount) {
            this.followerCount = followerCount;
        }
    }
}

