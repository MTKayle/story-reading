package org.example.storyreading.commentservice.dto.reaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.storyreading.commentservice.entity.Reaction.ReactionType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionRequest {
    private Long userId;
    private Long commentId;
    private ReactionType type;
    private Long authorId;
    private  Long storyId;
}
