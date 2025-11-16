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
    // Chỉ cho phép xóa trong vòng 5 phút sau khi đăng và chỉ chủ bình luận mới được xóa
    @PutMapping("/{id}/delete")
    public ResponseEntity<?> deleteComment(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        try {
            System.out.println("Delete comment request - id: " + id + ", body: " + body);
            
            if (body == null || body.get("userId") == null) {
                System.err.println("Missing userId in request body");
                return ResponseEntity.badRequest().body(Map.of("error", "userId là bắt buộc"));
            }
            
            Long userId;
            try {
                Object userIdObj = body.get("userId");
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else {
                    userId = Long.valueOf(userIdObj.toString());
                }
                System.out.println("Parsed userId: " + userId);
            } catch (NumberFormatException e) {
                System.err.println("Invalid userId format: " + e.getMessage());
                return ResponseEntity.badRequest().body(Map.of("error", "userId không hợp lệ"));
            }
            
            System.out.println("Calling deleteComment service - id: " + id + ", userId: " + userId);
            Comment deleted = commentService.deleteComment(id, userId);
            System.out.println("Comment deleted successfully: " + deleted.getId());
            
            return ResponseEntity.ok(deleted);
        } catch (RuntimeException e) {
            System.err.println("RuntimeException in deleteComment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Exception in deleteComment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi khi xóa bình luận: " + e.getMessage()));
        }
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
    public ResponseEntity<List<CommentResponse>> getRootCommentsByStoryId(@PathVariable Long storyId) {
        List<CommentResponse> rootComments = commentService.getRootCommentsByStoryId(storyId);
        return ResponseEntity.ok(rootComments);
    }
}


