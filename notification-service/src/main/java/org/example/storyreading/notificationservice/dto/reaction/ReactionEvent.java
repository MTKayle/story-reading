package org.example.storyreading.notificationservice.dto.reaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionEvent implements Serializable {
    private Long reactionId;
    private Long userId;
    private Long commentId;
    private Long authorId;
    private String type; // LIKE, DISLIKE, REPORT
    private Long storyId;
}

