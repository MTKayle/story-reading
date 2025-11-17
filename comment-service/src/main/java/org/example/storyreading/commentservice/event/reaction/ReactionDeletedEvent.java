package org.example.storyreading.commentservice.event.reaction;

import java.io.Serial;
import java.io.Serializable;

public class ReactionDeletedEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long reactionId;

    public ReactionDeletedEvent() {}

    public ReactionDeletedEvent(Long reactionId) {
        this.reactionId = reactionId;
    }

    public Long getReactionId() {
        return reactionId;
    }

    public void setReactionId(Long reactionId) {
        this.reactionId = reactionId;
    }
}
