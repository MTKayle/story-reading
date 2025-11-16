package org.example.storyreading.commentservice.controller;


import lombok.RequiredArgsConstructor;
import org.example.storyreading.commentservice.dto.reaction.ReactionRequest;
import org.example.storyreading.commentservice.dto.reaction.ReactionResponse;
import org.example.storyreading.commentservice.entity.Reaction;
import org.example.storyreading.commentservice.service.ReactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reaction")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    @PostMapping
    public ResponseEntity<ReactionResponse> react(@RequestBody ReactionRequest request){
        return ResponseEntity.ok(reactionService.react(request));
    }

    @DeleteMapping
    public ResponseEntity<?> remove(@RequestParam Long userId, @RequestParam Long commentId){
        reactionService.removeReaction(userId, commentId);
        return ResponseEntity.ok("Reaction removed");
    }

    @PostMapping("{commentId}")
    public ResponseEntity<?> getReactionCounts(@PathVariable Long commentId) {
        return ResponseEntity.ok(reactionService.getReactionCounts(commentId));
    }

    @GetMapping
    public ResponseEntity<?> getUserReaction(@RequestParam Long userId, @RequestParam Long commentId) {
        try {
            System.out.println("getUserReaction request - userId: " + userId + ", commentId: " + commentId);
            
            if (userId == null || commentId == null) {
                System.err.println("getUserReaction: Missing userId or commentId");
                return ResponseEntity.badRequest().body(Map.of("error", "userId và commentId là bắt buộc"));
            }
            
            Reaction reaction = reactionService.getUserReaction(userId, commentId);
            
            if (reaction == null) {
                System.out.println("getUserReaction: No reaction found, returning null response");
                // Trả về response với type là null thay vì dùng Map.of() với null
                Map<String, Object> response = new HashMap<>();
                response.put("userId", userId);
                response.put("commentId", commentId);
                response.put("type", null);
                return ResponseEntity.ok(response);
            }
            
            System.out.println("getUserReaction: Returning reaction type=" + reaction.getType());
            Map<String, Object> response = new HashMap<>();
            response.put("userId", reaction.getUserId());
            response.put("commentId", reaction.getCommentId());
            response.put("type", reaction.getType() != null ? reaction.getType().name() : null);
            response.put("id", reaction.getId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Exception in getUserReaction: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi khi lấy reaction: " + e.getMessage()));
        }
    }
}


