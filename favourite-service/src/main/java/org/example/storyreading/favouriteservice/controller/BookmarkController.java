package org.example.storyreading.favouriteservice.controller;

import org.example.storyreading.favouriteservice.dto.BookmarkDto;
import org.example.storyreading.favouriteservice.service.BookmarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    @Autowired
    private BookmarkService bookmarkService;

    /**
     * Đánh dấu hoặc cập nhật vị trí đọc của user cho một truyện
     */
    @PostMapping
    public ResponseEntity<BookmarkDto.BookmarkResponse> createOrUpdateBookmark(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody BookmarkDto.BookmarkRequest request) {
        BookmarkDto.BookmarkResponse response = bookmarkService.createOrUpdateBookmark(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy vị trí đọc hiện tại của user cho một truyện cụ thể
     */
    @GetMapping("/story/{storyId}")
    public ResponseEntity<BookmarkDto.BookmarkResponse> getBookmark(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long storyId) {
        return bookmarkService.getBookmark(userId, storyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lấy tất cả các bookmark (lịch sử đọc) của user
     */
    @GetMapping
    public ResponseEntity<List<BookmarkDto.BookmarkResponse>> getUserBookmarks(
            @RequestHeader("X-User-Id") Long userId) {
        List<BookmarkDto.BookmarkResponse> bookmarks = bookmarkService.getUserBookmarks(userId);
        return ResponseEntity.ok(bookmarks);
    }

    /**
     * Xóa bookmark của user cho một truyện
     */
    @DeleteMapping("/story/{storyId}")
    public ResponseEntity<Void> deleteBookmark(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long storyId) {
        bookmarkService.deleteBookmark(userId, storyId);
        return ResponseEntity.noContent().build();
    }
}

