package org.example.storyreading.commentservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.storyreading.commentservice.dto.comment.CommentRequest;
import org.example.storyreading.commentservice.dto.comment.CommentResponse;
import org.example.storyreading.commentservice.entity.Comment;
import org.example.storyreading.commentservice.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public CommentResponse createComment(@RequestBody CommentRequest request) {
        return commentService.createComment(request);
    }

    @GetMapping("/chapter/{chapterId}/story/{storyId}")
    public List<CommentResponse> getComments(@PathVariable Long chapterId, @PathVariable Long storyId) {
        return commentService.getCommentsByChapterAndStory(chapterId,storyId);
    }
    // sửa nội dung bình luận
    @PutMapping("/{id}")
    public ResponseEntity<Comment> updateComment(@PathVariable Long id,
                                                 @RequestBody Map<String, String> body) {
        String newContent = body.get("content");
        Comment updated = commentService.updateComment(id, newContent);
        return ResponseEntity.ok(updated);
    }

    // xóa bình luận (đặt isDeleted = "Yes")
    @PutMapping("/{id}/delete")
    public ResponseEntity<Comment> deleteComment(@PathVariable Long id) {
        Comment deleted = commentService.deleteComment(id);
        return ResponseEntity.ok(deleted);
    }

    // chặn bình luận (đặt isDeleted = "Blocked")
    @PutMapping("/{id}/block")
    public ResponseEntity<Comment> blockComment(@PathVariable Long id) {
        Comment blocked = commentService.blockComment(id);
        return ResponseEntity.ok(blocked);
    }

    @PostMapping("delete/story/{storyId}")
    public ResponseEntity<String> deleteCommentsByStory(@PathVariable Long storyId) {
        commentService.deleteCommentsByStoryId(storyId);
        return ResponseEntity.ok("Xóa tất cả bình luận của truyện thành công");
    }

    @GetMapping("/story/{storyId}/root")
    public ResponseEntity<List<Comment>> getRootCommentsByStoryId(@PathVariable Long storyId) {
        List<Comment> rootComments = commentService.getRootCommentsByStoryId(storyId);
        return ResponseEntity.ok(rootComments);
    }

    @GetMapping("/parent/{parentId}/replies")
    public ResponseEntity<List<CommentResponse>> getRepliesByParentId(@PathVariable Long parentId) {
        List<CommentResponse> replies = commentService.getRepliesByParentId(parentId);
        return ResponseEntity.ok(replies);
    }
}


