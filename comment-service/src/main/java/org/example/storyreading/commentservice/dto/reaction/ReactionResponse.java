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
public class ReactionResponse {
    private Long commentId;
    private ReactionType type;
    private Long userId;

    // Số lượng từng loại reaction
    private Long likeCount;
    private Long tymCount;
    private Long hahaCount;
    private Long sadCount;
    private Long angryCount;
    private Long wowCount;
}
