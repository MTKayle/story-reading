package org.example.storyreading.commentservice.controller;


import lombok.RequiredArgsConstructor;
import org.example.storyreading.commentservice.dto.rating.RatingRequest;
import org.example.storyreading.commentservice.dto.rating.RatingResponse;
import org.example.storyreading.commentservice.service.RatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rating")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public ResponseEntity<RatingResponse> rate(@RequestBody RatingRequest request){
        return ResponseEntity.ok(ratingService.rate(request));
    }

    @DeleteMapping
    public ResponseEntity<?> remove(@RequestParam Long userId, @RequestParam Long storyId){
        ratingService.removeRating(userId, storyId);
        return ResponseEntity.ok("Rating removed");
    }

    @GetMapping("/{storyId}")
    public ResponseEntity<RatingResponse> getRating(@PathVariable Long storyId){
        return ResponseEntity.ok(ratingService.getRating(storyId));
    }

    @GetMapping("/user/{userId}/story/{storyId}")
    public ResponseEntity<?> getUserRating(@PathVariable Long userId, @PathVariable Long storyId){
        Integer stars = ratingService.getUserRating(userId, storyId);
        return ResponseEntity.ok(java.util.Map.of("stars", stars != null ? stars : 0));
    }
}


