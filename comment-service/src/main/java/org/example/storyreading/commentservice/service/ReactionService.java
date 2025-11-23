package org.example.storyreading.commentservice.service;

import org.example.storyreading.commentservice.dto.reaction.ReactionRequest;
import org.example.storyreading.commentservice.dto.reaction.ReactionResponse;
import org.example.storyreading.commentservice.entity.Reaction;

import java.util.List;
import java.util.Map;

public interface ReactionService {
    ReactionResponse react(ReactionRequest request); // like/dislike/report
    void removeReaction(Long userId, Long commentId); // bỏ reaction
    List<Long> getReactionIdByCommentId(Long commentId);
    void removeReactionById (Long reactionId);
    Map<Reaction.ReactionType, Long> getReactionCounts(Long commentId);
    Reaction.ReactionType getUserReaction(Long userId, Long commentId); // lấy reaction của user cho comment
}

