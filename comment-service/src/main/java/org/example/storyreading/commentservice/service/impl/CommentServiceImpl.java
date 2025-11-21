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
        // Validate parentId nếu có (đảm bảo parent comment tồn tại)
        if (request.getParentId() != null) {
            boolean parentExists = commentRepository.existsById(request.getParentId());
            if (!parentExists) {
                throw new RuntimeException("Parent comment không tồn tại với id: " + request.getParentId());
            }
            System.out.println("✅ Tạo reply cho comment id: " + request.getParentId());
        }

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
        
        // Log để đảm bảo parentId được lưu đúng
        if (saved.getParentId() != null) {
            System.out.println("✅ Reply đã được lưu vào database với id: " + saved.getId() + ", parentId: " + saved.getParentId());
        } else {
            System.out.println("✅ Root comment đã được lưu vào database với id: " + saved.getId());
        }

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
        String channelId = saved.getChapterId() != null ? saved.getChapterId().toString() : "story-" + saved.getStoryId();
        messagingTemplate.convertAndSend("/topic/comments/" + channelId, response);

        // Tạo event để gửi sang notification-service qua RabbitMQ
        // Lấy parentUserId nếu là reply
        Long parentUserId = null;
        if (saved.getParentId() != null) {
            parentUserId = commentRepository.findUserIdByCommentId(saved.getParentId());
            System.out.println("📝 Reply detected - ParentId: " + saved.getParentId() + ", ParentUserId: " + parentUserId);
        }
        
        Long storyAuthorId = request.getStoryAuthorId();
        if (storyAuthorId == null) {
            System.out.println("⚠️ Warning: storyAuthorId is null - notification may not be sent to story author");
        }
        
        CommentEvent event = new CommentEvent(
                saved.getId(),
                saved.getContent(),
                saved.getUserId(),
                saved.getParentId(),
                parentUserId,
                saved.getStoryId(),
                storyAuthorId
        );

        System.out.println("📤 Publishing comment event to RabbitMQ:");
        System.out.println("  - CommentId: " + event.getCommentId());
        System.out.println("  - UserId: " + event.getUserId());
        System.out.println("  - StoryId: " + event.getStoryId());
        System.out.println("  - AuthorId: " + event.getAuthorId());
        System.out.println("  - ParentId: " + event.getParentId());
        System.out.println("  - ParentUserId: " + event.getParentUserId());
        
        eventPublisher.publishCommentEvent(event);
        System.out.println("✅ Comment event published successfully");
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

        String channelId = saved.getChapterId() != null ? saved.getChapterId().toString() : "story-" + saved.getStoryId();
        messagingTemplate.convertAndSend("/topic/comments/" + channelId, payload);

        return saved;
    }

    @Transactional
    @Override
    public Comment deleteComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận có id = " + id));

        // 1. Lấy tất cả comment con (tìm theo parentId = comment.getId())
        List<Comment> childComments = commentRepository.findByParentIdAndIsDeleted(comment.getId(), "No");
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
        String channelId = saved.getChapterId() != null ? saved.getChapterId().toString() : "story-" + saved.getStoryId();
        messagingTemplate.convertAndSend("/topic/comments/" + channelId, payload);

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

        String channelId = saved.getChapterId() != null ? saved.getChapterId().toString() : "story-" + saved.getStoryId();
        messagingTemplate.convertAndSend("/topic/comments/" + channelId, payload);
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
        // Chỉ lấy root comments: chapterId IS NULL và parentId IS NULL
        return commentRepository.findByStoryIdAndChapterIdIsNullAndIsDeletedOrderByCreatedAtAsc(storyId, "No")
                .stream()
                .filter(c -> c.getParentId() == null)  // Chỉ lấy root comments, không lấy replies
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentResponse> getRepliesByParentId(Long parentId) {
        return commentRepository.findByParentIdAndIsDeleted(parentId, "No")
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



