package org.example.storyreading.commentservice.service.impl;

//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import org.example.storyreading.commentservice.dto.reaction.ReactionRequest;
//import org.example.storyreading.commentservice.dto.reaction.ReactionResponse;
//import org.example.storyreading.commentservice.entity.Reaction;
//import org.example.storyreading.commentservice.entity.Reaction.ReactionType;
//import org.example.storyreading.commentservice.event.reaction.ReactionDeletedEvent;
//import org.example.storyreading.commentservice.event.reaction.ReactionEventPublisher;
//import org.example.storyreading.commentservice.event.reaction.ReactionEvent;
//import org.example.storyreading.commentservice.repository.ReactionRepository;
//import org.example.storyreading.commentservice.service.ReactionService;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Service;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.storyreading.commentservice.dto.reaction.ReactionRequest;
import org.example.storyreading.commentservice.dto.reaction.ReactionResponse;
import org.example.storyreading.commentservice.entity.Reaction;
import org.example.storyreading.commentservice.entity.Reaction.ReactionType;
import org.example.storyreading.commentservice.event.reaction.ReactionDeletedEvent;
import org.example.storyreading.commentservice.event.reaction.ReactionEventPublisher;
import org.example.storyreading.commentservice.event.reaction.ReactionEvent;
import org.example.storyreading.commentservice.repository.ReactionRepository;
import org.example.storyreading.commentservice.service.ReactionService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReactionServiceImpl implements ReactionService {

    private final ReactionRepository reactionRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ReactionEventPublisher reactionEventPublisher;

    @Override
    public ReactionResponse react(ReactionRequest request) {
        Reaction reaction = reactionRepository.findByUserIdAndCommentId(request.getUserId(), request.getCommentId())
                .orElse(Reaction.builder()
                        .userId(request.getUserId())
                        .commentId(request.getCommentId())
                        .build());

        reaction.setType(request.getType());
        Reaction saved = reactionRepository.save(reaction);

        reactionEventPublisher.publishReactionEvent(
                new ReactionEvent(reaction.getId(), reaction.getCommentId(), reaction.getUserId(),
                        reaction.getType().name(), request.getAuthorId(), request.getStoryId())
        );

        // Đếm tất cả các loại reaction
        Map<ReactionType, Long> reactionCounts = getReactionCounts(saved.getCommentId());

        // Realtime
        ReactionResponse response = ReactionResponse.builder()
                .commentId(saved.getCommentId())
                .type(saved.getType())
                .userId(saved.getUserId())
                .likeCount(reactionCounts.get(ReactionType.LIKE))
                .tymCount(reactionCounts.get(ReactionType.TYM))
                .hahaCount(reactionCounts.get(ReactionType.HAHA))
                .sadCount(reactionCounts.get(ReactionType.SAD))
                .angryCount(reactionCounts.get(ReactionType.ANGRY))
                .wowCount(reactionCounts.get(ReactionType.WOW))
                .build();

        messagingTemplate.convertAndSend("/topic/comments/reaction/" + saved.getCommentId(), response);

        return response;
    }

    @Override
    @Transactional
    public void removeReaction(Long userId, Long commentId) {
        // Lấy reaction trước khi xóa
        Reaction reaction = reactionRepository.findByUserIdAndCommentId(userId, commentId)
                .orElseThrow(() -> new RuntimeException("Reaction not found"));

        Long reactionId = reaction.getId();

        // Xóa reaction
        reactionRepository.delete(reaction);

        // Tính lại số lượng các loại reaction
        Map<ReactionType, Long> reactionCounts = getReactionCounts(commentId);

        // Gửi realtime
        Map<String, Object> payload = new HashMap<>();
        payload.put("commentId", commentId);
        payload.put("type", null);
        payload.put("userId", userId);
        payload.put("likeCount", reactionCounts.get(ReactionType.LIKE));
        payload.put("tymCount", reactionCounts.get(ReactionType.TYM));
        payload.put("hahaCount", reactionCounts.get(ReactionType.HAHA));
        payload.put("sadCount", reactionCounts.get(ReactionType.SAD));
        payload.put("angryCount", reactionCounts.get(ReactionType.ANGRY));
        payload.put("wowCount", reactionCounts.get(ReactionType.WOW));

        messagingTemplate.convertAndSend("/topic/comments/reaction/" + commentId, payload);

        // Phát event sang notification-service
        reactionEventPublisher.publishReactionDeletedEvent(
                new ReactionDeletedEvent(reactionId)
        );
    }

    @Override
    public void removeReactionById(Long reactionId) {
        reactionRepository.deleteById(reactionId);
    }

    @Override
    @Transactional
    public List<Long> getReactionIdByCommentId(Long commentId) {
        return reactionRepository.findIdsByCommentIdIn(commentId);
    }

    /**
     * Helper method để đếm tất cả các loại reaction của một comment
     */
    @Override
    public Map<ReactionType, Long> getReactionCounts(Long commentId) {
        Map<ReactionType, Long> counts = new HashMap<>();

        for (ReactionType type : ReactionType.values()) {
            long count = reactionRepository.countByCommentIdAndType(commentId, type);
            counts.put(type, count);
        }
        return counts;
    }

    @Override
    public Reaction getUserReaction(Long userId, Long commentId) {
        System.out.println("getUserReaction called - userId: " + userId + ", commentId: " + commentId);
        
        if (userId == null || commentId == null) {
            System.err.println("getUserReaction: userId or commentId is null");
            return null;
        }
        
        Reaction reaction = reactionRepository.findByUserIdAndCommentId(userId, commentId)
                .orElse(null);
        
        if (reaction == null) {
            System.out.println("getUserReaction: No reaction found for userId=" + userId + ", commentId=" + commentId);
        } else {
            System.out.println("getUserReaction: Found reaction id=" + reaction.getId() + ", type=" + reaction.getType());
        }
        
        return reaction;
    }
}
