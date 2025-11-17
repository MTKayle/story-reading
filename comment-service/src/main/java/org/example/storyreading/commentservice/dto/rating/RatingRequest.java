package org.example.storyreading.commentservice.dto.rating;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingRequest {
    private Long userId;
    private Long storyId;
    private int stars; // 1 -> 5
    private Long storyAuthorId;
}