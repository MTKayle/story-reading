package org.example.storyreading.notificationservice.dto.comment;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class CommentDeletedEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<Long> reactionIds;
    private Long commentId;

    public CommentDeletedEvent() {}

    public CommentDeletedEvent(Long commentId, List<Long> reactionIds) {
        this.reactionIds = reactionIds;
        this.commentId = commentId;
    }

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }
    public List<Long> getReactionIds() {
        return reactionIds;
    }
    public void setReactionIds(List<Long> reactionIds) {
        this.reactionIds = reactionIds;
    }
}