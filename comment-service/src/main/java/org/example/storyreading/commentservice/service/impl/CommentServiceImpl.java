package org.example.storyreading.commentservice.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.storyreading.commentservice.dto.comment.CommentRequest;
import org.example.storyreading.commentservice.dto.comment.CommentResponse;
import org.example.storyreading.commentservice.dto.comment.CommentWithReportCountResponse;
import org.example.storyreading.commentservice.entity.Comment;
import org.example.storyreading.commentservice.event.comment.CommentDeletedEvent;
import org.example.storyreading.commentservice.event.comment.CommentEventPublisher;
import org.example.storyreading.commentservice.event.comment.CommentEvent;
import org.example.storyreading.commentservice.repository.CommentRepository;
import org.example.storyreading.commentservice.service.CommentService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

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
    private final ReactionServiceImpl reactionService;
    private final RatingServiceImpl ratingService;

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

        // Gửi realtime comment mới qua WebSocket
        messagingTemplate.convertAndSend("/topic/comments/" + saved.getChapterId(), response);

        // Tạo event để gửi sang notification-service qua RabbitMQ
        CommentEvent event = new CommentEvent(
                saved.getId(),
                saved.getContent(),
                saved.getUserId(),
                saved.getParentId(),
                saved.getStoryId(),
                request.getStoryAuthorId() // TODO: Lấy authorId của truyện từ service StoryService
        );

        eventPublisher.publishCommentEvent(event);
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
    public Comment deleteComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận có id = " + id));

        // 1. Lấy tất cả comment con
        List<Comment> childComments = commentRepository.findByParentIdAndIsDeleted(comment.getUserId(), "No");
        for (Comment child : childComments) {
            // 2. Xóa đệ quy từng comment con
            deleteComment(child.getId());
        }

        // 3. Xóa comment hiện tại
        comment.setIsDeleted("Yes");
        Comment saved = commentRepository.save(comment);

        // 4. Xóa reaction liên quan
        List<Long> reactionIds = reactionService.getReactionIdByCommentId(id);
        for (Long reactionId : reactionIds) {
            reactionService.removeReactionById(reactionId);
        }

        // 5. Gửi WebSocket realtime
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "delete");
        payload.put("comment", saved);
        messagingTemplate.convertAndSend("/topic/comments/" + saved.getChapterId(), payload);

        // 6. Gửi event RabbitMQ
        eventPublisher.publishCommentDeletedEvent(new CommentDeletedEvent(saved.getId(), reactionIds));

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
            deleteComment(comment.getId());
        }
    }

    @Override
    public List<Comment> getRootCommentsByStoryId(Long storyId) {
        return commentRepository.findByStoryIdAndChapterIdIsNullAndIsDeletedOrderByCreatedAtAsc(storyId, "No");
    }

    @Override
    public Long getUserIdByCommentId(Long commentId) {
        Long userId = commentRepository.findUserIdByCommentId(commentId);
        if (userId == null) {
            throw new RuntimeException("Không tìm thấy bình luận có id = " + commentId);
        }
        return userId;
    }

    @Override
    public List<CommentWithReportCountResponse> getAllCommentsWithReportsSortedByCount() {
        // 1. Lấy tất cả comment có report
        List<Comment> commentsWithReports = commentRepository.findAllCommentsWithReports();

        // 2. Map sang DTO và đếm số lượng report cho mỗi comment
        List<CommentWithReportCountResponse> result = commentsWithReports.stream()
                .map(comment -> {
                    long reportCount = reactionService.getReportCount(comment.getId());
                    return CommentWithReportCountResponse.builder()
                            .id(comment.getId())
                            .storyId(comment.getStoryId())
                            .chapterId(comment.getChapterId())
                            .userId(comment.getUserId())
                            .parentId(comment.getParentId())
                            .content(comment.getContent())
                            .isDeleted(comment.getIsDeleted())
                            .createdAt(comment.getCreatedAt())
                            .updatedAt(comment.getUpdatedAt())
                            .reportCount(reportCount)
                            .build();
                })
                // 3. Sắp xếp giảm dần theo số lượng report
                .sorted((c1, c2) -> Long.compare(c2.getReportCount(), c1.getReportCount()))
                .collect(Collectors.toList());

        return result;
    }
}
