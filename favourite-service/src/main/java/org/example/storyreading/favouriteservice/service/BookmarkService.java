package org.example.storyreading.favouriteservice.service;

import org.example.storyreading.favouriteservice.dto.BookmarkDto;
import org.example.storyreading.favouriteservice.entity.Bookmark;
import org.example.storyreading.favouriteservice.repository.BookmarkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookmarkService {

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Transactional
    public BookmarkDto.BookmarkResponse createOrUpdateBookmark(Long userId, BookmarkDto.BookmarkRequest request) {
        Optional<Bookmark> existingBookmark = bookmarkRepository.findByUserIdAndStoryId(userId, request.getStoryId());

        Bookmark bookmark;
        if (existingBookmark.isPresent()) {
            bookmark = existingBookmark.get();
            bookmark.setChapterId(request.getChapterId());
            bookmark.setChapterNumber(request.getChapterNumber());
        } else {
            bookmark = new Bookmark();
            bookmark.setUserId(userId);
            bookmark.setStoryId(request.getStoryId());
            bookmark.setChapterId(request.getChapterId());
            bookmark.setChapterNumber(request.getChapterNumber());
        }

        bookmark = bookmarkRepository.save(bookmark);
        return mapToResponse(bookmark);
    }

    public Optional<BookmarkDto.BookmarkResponse> getBookmark(Long userId, Long storyId) {
        return bookmarkRepository.findByUserIdAndStoryId(userId, storyId)
                .map(this::mapToResponse);
    }

    public List<BookmarkDto.BookmarkResponse> getUserBookmarks(Long userId) {
        return bookmarkRepository.findByUserIdOrderByLastReadAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteBookmark(Long userId, Long storyId) {
        bookmarkRepository.deleteByUserIdAndStoryId(userId, storyId);
    }

    private BookmarkDto.BookmarkResponse mapToResponse(Bookmark bookmark) {
        BookmarkDto.BookmarkResponse response = new BookmarkDto.BookmarkResponse();
        response.setId(bookmark.getId());
        response.setUserId(bookmark.getUserId());
        response.setStoryId(bookmark.getStoryId());
        response.setChapterId(bookmark.getChapterId());
        response.setChapterNumber(bookmark.getChapterNumber());
        response.setLastReadAt(bookmark.getLastReadAt());
        response.setCreatedAt(bookmark.getCreatedAt());
        response.setUpdatedAt(bookmark.getUpdatedAt());
        return response;
    }
}

