package org.example.storyreading.userservice.service;

import org.example.storyreading.userservice.entity.BookmarkEntity;

import java.util.List;

public interface BookmarkService {
    boolean addBookmark(Long userId, Long storyId, Long chapterId);
    boolean removeBookmark(Long userId, Long storyId, Long chapterId);
    boolean isBookmarked(Long userId, Long storyId, Long chapterId);
    List<BookmarkEntity> getUserBookmarks(Long userId);
    List<BookmarkEntity> getStoryBookmarks(Long userId, Long storyId);
}

