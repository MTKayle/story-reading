package org.example.storyreading.commentservice.controller;


import lombok.RequiredArgsConstructor;
import org.example.storyreading.commentservice.dto.reaction.ReactionRequest;
import org.example.storyreading.commentservice.dto.reaction.ReactionResponse;
import org.example.storyreading.commentservice.entity.Reaction;
import org.example.storyreading.commentservice.service.ReactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // ✅ Tạo report cho comment (sử dụng ReactionType.REPORT)
    @PostMapping("/report")
    public ResponseEntity<ReactionResponse> createReport(@RequestParam Long userId, @RequestParam Long commentId) {
        return ResponseEntity.ok(reactionService.createReport(userId, commentId));
    }

    // ✅ Hủy report
    @DeleteMapping("/report")
    public ResponseEntity<String> removeReport(@RequestParam Long userId, @RequestParam Long commentId) {
        reactionService.removeReport(userId, commentId);
        return ResponseEntity.ok("Report đã được hủy");
    }

    // ✅ Lấy danh sách report của 1 comment
    @GetMapping("/report/{commentId}")
    public ResponseEntity<List<Reaction>> getReportsByComment(@PathVariable Long commentId) {
        return ResponseEntity.ok(reactionService.getReportsByCommentId(commentId));
    }

    // ✅ Lấy số lượng report của 1 comment
    @GetMapping("/report/{commentId}/count")
    public ResponseEntity<Long> getReportCount(@PathVariable Long commentId) {
        return ResponseEntity.ok(reactionService.getReportCount(commentId));
    }
}
