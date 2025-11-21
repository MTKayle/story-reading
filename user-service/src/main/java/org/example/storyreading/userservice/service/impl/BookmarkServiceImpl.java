package org.example.storyreading.userservice.service.impl;

import org.example.storyreading.userservice.entity.BookmarkEntity;
import org.example.storyreading.userservice.entity.UserEntity;
import org.example.storyreading.userservice.repository.BookmarkRepository;
import org.example.storyreading.userservice.repository.UserRepository;
import org.example.storyreading.userservice.service.BookmarkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BookmarkServiceImpl implements BookmarkService {

    private static final Logger log = LoggerFactory.getLogger(BookmarkServiceImpl.class);
    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;

    public BookmarkServiceImpl(BookmarkRepository bookmarkRepository, UserRepository userRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public boolean addBookmark(Long userId, Long storyId, Long chapterId) {
        log.info("🔍 Checking if user {} already bookmarked story {} chapter {}", userId, storyId, chapterId);
        
        // Check if already bookmarked
        if (bookmarkRepository.existsByUserIdAndStoryIdAndChapterId(userId, storyId, chapterId)) {
            log.info("ℹ️ User {} already bookmarked story {} chapter {}", userId, storyId, chapterId);
            return false; // Already bookmarked
        }

        log.info("🔍 Loading user entity for userId: {}", userId);
        // Get user
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.error("❌ User not found with id: {}", userId);
            throw new RuntimeException("User not found with id: " + userId);
        }

        log.info("✅ User found, creating bookmark entity - userId: {}, storyId: {}, chapterId: {}", userId, storyId, chapterId);
        // Create bookmark entity
        try {
            BookmarkEntity bookmark = new BookmarkEntity(userOpt.get(), storyId, chapterId);
            bookmarkRepository.save(bookmark);
            log.info("✅ Successfully saved bookmark entity - userId: {}, storyId: {}, chapterId: {}", userId, storyId, chapterId);
            return true;
        } catch (Exception e) {
            log.error("❌ Error saving bookmark entity - userId: {}, storyId: {}, chapterId: {}", userId, storyId, chapterId, e);
            throw new RuntimeException("Failed to save bookmark: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean removeBookmark(Long userId, Long storyId, Long chapterId) {
        Optional<BookmarkEntity> bookmarkOpt = bookmarkRepository.findByUserIdAndStoryIdAndChapterId(userId, storyId, chapterId);
        if (bookmarkOpt.isEmpty()) {
            log.info("ℹ️ Bookmark not found - userId: {}, storyId: {}, chapterId: {}", userId, storyId, chapterId);
            return false; // Not bookmarked
        }

        bookmarkRepository.delete(bookmarkOpt.get());
        log.info("✅ Successfully removed bookmark - userId: {}, storyId: {}, chapterId: {}", userId, storyId, chapterId);
        return true;
    }

    @Override
    public boolean isBookmarked(Long userId, Long storyId, Long chapterId) {
        return bookmarkRepository.existsByUserIdAndStoryIdAndChapterId(userId, storyId, chapterId);
    }

    @Override
    public List<BookmarkEntity> getUserBookmarks(Long userId) {
        return bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<BookmarkEntity> getStoryBookmarks(Long userId, Long storyId) {
        return bookmarkRepository.findByUserIdAndStoryId(userId, storyId);
    }
}

