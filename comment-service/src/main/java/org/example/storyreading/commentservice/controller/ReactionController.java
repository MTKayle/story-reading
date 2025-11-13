package org.example.storyreading.commentservice.controller;


import lombok.RequiredArgsConstructor;
import org.example.storyreading.commentservice.dto.reaction.ReactionRequest;
import org.example.storyreading.commentservice.dto.reaction.ReactionResponse;
import org.example.storyreading.commentservice.service.ReactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}


