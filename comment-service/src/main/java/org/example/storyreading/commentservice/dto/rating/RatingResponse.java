package org.example.storyreading.commentservice.dto.rating;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingResponse {
    private Long storyId;
    private Double averageStars;
}
