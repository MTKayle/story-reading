package org.example.storyreading.commentservice.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.storyreading.commentservice.dto.comment.CommentRequest;
import org.example.storyreading.commentservice.dto.comment.CommentResponse;
import org.example.storyreading.commentservice.entity.Comment;
import org.example.storyreading.commentservice.event.comment.CommentDeletedEvent;
import org.example.storyreading.commentservice.event.comment.CommentEventPublisher;
import org.example.storyreading.commentservice.event.comment.CommentEvent;
import org.example.storyreading.commentservice.repository.CommentRepository;
import org.example.storyreading.commentservice.service.CommentService;
import org.example.storyreading.commentservice.service.ReactionService;
import org.example.storyreading.commentservice.service.RatingService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final SimpMessagingTemplate messagingTemplate; // WebSocket
    private final CommentEventPublisher eventPublisher;   // RabbitMQ
    private final ReactionService reactionService;
    private final RatingService ratingService;

    @Override
    public CommentResponse createComment(CommentRequest request) {
        // Tạo entity mới
        Comment comment = Comment.builder()
                .storyId(request.getStoryId())
                .chapterId(request.getChapterId())
                .userId(request.getUserId())
                .parentId(request.getParentId())
                .content(request.getContent())
                .build();

        // Lưu vào DB
        Comment saved = commentRepository.save(comment);

        // Tạo response trả về client
        CommentResponse response = CommentResponse.builder()
                .id(saved.getId())
                .storyId(saved.getStoryId())
                .chapterId(saved.getChapterId())
                .userId(saved.getUserId())
                .parentId(saved.getParentId())
                .content(saved.getContent())
                .createdAt(saved.getCreatedAt())
                .storyAuthorId(request.getStoryAuthorId())
                .updatedAt(saved.getUpdatedAt())
                .build();

        // Gửi realtime comment mới qua WebSocket (nếu có chapterId)
        try {
            if (saved.getChapterId() != null) {
                messagingTemplate.convertAndSend("/topic/comments/" + saved.getChapterId(), response);
            } else {
                // Nếu là root comment (không có chapterId), gửi theo storyId
                messagingTemplate.convertAndSend("/topic/comments/story/" + saved.getStoryId(), response);
            }
        } catch (Exception e) {
            // Log error nhưng không fail request nếu WebSocket không available
            System.err.println("Failed to send WebSocket message: " + e.getMessage());
        }

        // Tạo event để gửi sang notification-service qua RabbitMQ
        try {
            CommentEvent event = new CommentEvent(
                    saved.getId(),
                    saved.getContent(),
                    saved.getUserId(),
                    saved.getParentId(),
                    saved.getStoryId(),
                    request.getStoryAuthorId() // TODO: Lấy authorId của truyện từ service StoryService
            );
            eventPublisher.publishCommentEvent(event);
        } catch (Exception e) {
            // Log error nhưng không fail request nếu RabbitMQ không available
            System.err.println("Failed to publish RabbitMQ event: " + e.getMessage());
        }
        
        return response;
    }

    @Transactional
    @Override
    public Comment updateComment(Long id, String newContent) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận có id = " + id));

        comment.setContent(newContent);
        Comment saved = commentRepository.save(comment);

        // 🔥 gửi dữ liệu realtime
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "update");
        payload.put("comment", saved);

        messagingTemplate.convertAndSend("/topic/comments/" + saved.getChapterId(), payload);

        return saved;
    }

    @Transactional
    @Override
    public Comment deleteComment(Long id, Long userId) {
        System.out.println("deleteComment called - id: " + id + ", userId: " + userId);
        
        if (id == null) {
            throw new RuntimeException("Comment id không được để trống");
        }
        if (userId == null) {
            throw new RuntimeException("User id không được để trống");
        }

        System.out.println("Finding comment with id: " + id);
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận có id = " + id));
        System.out.println("Found comment: " + comment.getId() + ", userId: " + comment.getUserId() + ", isDeleted: " + comment.getIsDeleted());

        // Kiểm tra comment đã bị xóa chưa
        if ("Yes".equals(comment.getIsDeleted()) || "Blocked".equals(comment.getIsDeleted())) {
            throw new RuntimeException("Bình luận này đã bị xóa hoặc bị chặn");
        }

        // Kiểm tra quyền sở hữu: chỉ chủ bình luận mới được xóa
        if (comment.getUserId() == null) {
            throw new RuntimeException("Bình luận không có thông tin người dùng");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa bình luận này");
        }

        // Kiểm tra thời gian: chỉ cho phép xóa trong vòng 5 phút
        if (comment.getCreatedAt() != null) {
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.between(comment.getCreatedAt(), now);
            long minutes = duration.toMinutes();
            
            if (minutes > 5) {
                throw new RuntimeException("Chỉ có thể xóa bình luận trong vòng 5 phút sau khi đăng");
            }
        }

        // Chỉ đổi isDeleted thành "Yes"
        System.out.println("Setting isDeleted to Yes for comment: " + comment.getId());
        comment.setIsDeleted("Yes");
        
        System.out.println("Saving comment...");
        Comment saved = commentRepository.save(comment);
        System.out.println("Comment saved successfully, id: " + saved.getId() + ", isDeleted: " + saved.getIsDeleted());
        
        // Flush để đảm bảo thay đổi được commit ngay
        System.out.println("Flushing repository...");
        commentRepository.flush();
        System.out.println("Repository flushed successfully");

        // Gửi WebSocket realtime (sau khi commit transaction)
        if (messagingTemplate != null) {
            try {
                System.out.println("Sending WebSocket message...");
                Map<String, Object> payload = new HashMap<>();
                payload.put("action", "delete");
                payload.put("commentId", saved.getId());
                payload.put("storyId", saved.getStoryId());
                payload.put("chapterId", saved.getChapterId());
                
                if (saved.getChapterId() != null) {
                    messagingTemplate.convertAndSend("/topic/comments/" + saved.getChapterId(), payload);
                } else if (saved.getStoryId() != null) {
                    messagingTemplate.convertAndSend("/topic/comments/story/" + saved.getStoryId(), payload);
                }
                System.out.println("WebSocket message sent successfully");
            } catch (Exception e) {
                // Log error nhưng không fail request nếu WebSocket không available
                System.err.println("Failed to send WebSocket message: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("messagingTemplate is null, skipping WebSocket");
        }

        System.out.println("deleteComment completed successfully, returning saved comment");
        return saved;
    }



    @Transactional
    @Override
    public Comment blockComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận có id = " + id));

        comment.setIsDeleted("Blocked");
        Comment saved = commentRepository.save(comment);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "block");
        payload.put("comment", saved);

        messagingTemplate.convertAndSend("/topic/comments/" + saved.getChapterId(), payload);
        // 🔥 Gửi RabbitMQ event chặn
        eventPublisher.publishCommentDeletedEvent(new CommentDeletedEvent(saved.getId(), reactionService.getReactionIdByCommentId(id)));
        return saved;
    }

    @Override
    public List<CommentResponse> getCommentsByChapterAndStory(Long chapterId, Long storyId) {
        return commentRepository.findByChapterIdAndStoryIdAndIsDeletedOrderByCreatedAtAsc(chapterId, storyId, "No")
                .stream()
                .map(c -> CommentResponse.builder()
                        .id(c.getId())
                        .storyId(c.getStoryId())
                        .chapterId(c.getChapterId())
                        .userId(c.getUserId())
                        .parentId(c.getParentId())
                        .content(c.getContent())
                        .createdAt(c.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    // Nếu xóa truyện thì xóa hết bình luận liên quan và reaction và rating
    public void deleteCommentsByStoryId(Long storyId) {
        ratingService.deleteRatingsByStoryId(storyId);
        List<Comment> comments = commentRepository.findByStoryIdAndIsDeletedOrderByCreatedAtAsc(storyId, "No");
        for (Comment comment : comments) {
            // Admin xóa tất cả comment khi xóa truyện, không cần kiểm tra quyền và thời gian
            comment.setIsDeleted("Yes");
            commentRepository.save(comment);
        }
    }

    @Override
    public List<CommentResponse> getRootCommentsByStoryId(Long storyId) {
        return commentRepository.findByStoryIdAndChapterIdIsNullAndIsDeletedOrderByCreatedAtAsc(storyId, "No")
                .stream()
                .map(c -> CommentResponse.builder()
                        .id(c.getId())
                        .storyId(c.getStoryId())
                        .chapterId(c.getChapterId())
                        .userId(c.getUserId())
                        .parentId(c.getParentId())
                        .content(c.getContent())
                        .createdAt(c.getCreatedAt())
                        .updatedAt(c.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}



