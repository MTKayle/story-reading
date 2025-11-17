package org.example.storyreading.favouriteservice.dto;

import java.time.LocalDateTime;

public class BookmarkDto {

    public static class BookmarkRequest {
        private Long storyId;
        private Long chapterId;
        private Integer chapterNumber;

        public Long getStoryId() {
            return storyId;
        }

        public void setStoryId(Long storyId) {
            this.storyId = storyId;
        }

        public Long getChapterId() {
            return chapterId;
        }

        public void setChapterId(Long chapterId) {
            this.chapterId = chapterId;
        }

        public Integer getChapterNumber() {
            return chapterNumber;
        }

        public void setChapterNumber(Integer chapterNumber) {
            this.chapterNumber = chapterNumber;
        }
    }

    public static class BookmarkResponse {
        private Long id;
        private Long userId;
        private Long storyId;
        private Long chapterId;
        private Integer chapterNumber;
        private LocalDateTime lastReadAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

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

        public Long getChapterId() {
            return chapterId;
        }

        public void setChapterId(Long chapterId) {
            this.chapterId = chapterId;
        }

        public Integer getChapterNumber() {
            return chapterNumber;
        }

        public void setChapterNumber(Integer chapterNumber) {
            this.chapterNumber = chapterNumber;
        }

        public LocalDateTime getLastReadAt() {
            return lastReadAt;
        }

        public void setLastReadAt(LocalDateTime lastReadAt) {
            this.lastReadAt = lastReadAt;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}

