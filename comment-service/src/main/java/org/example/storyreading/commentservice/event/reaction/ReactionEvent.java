package org.example.storyreading.commentservice.event.reaction;

import java.io.Serial;
import java.io.Serializable;

public class ReactionEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long reactionId;
    private Long commentId;
    private Long userId;      // người like/dislike/report
    private String type;      // LIKE, DISLIKE, REPORT
    private Long authorId;    // chủ comment hoặc chủ truyện được phản ứng
    private Long storyId;

    public ReactionEvent() {}

    public ReactionEvent(Long reactionId, Long commentId, Long userId, String type, Long authorId, Long storyId) {
        this.reactionId = reactionId;
        this.commentId = commentId;
        this.userId = userId;
        this.type = type;
        this.authorId = authorId;
        this.storyId = storyId;
    }

    // getters & setters
    public Long getReactionId() { return reactionId; }
    public void setReactionId(Long reactionId) { this.reactionId = reactionId; }
    public Long getCommentId() { return commentId; }
    public void setCommentId(Long commentId) { this.commentId = commentId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public Long getStoryId() { return storyId; }
    public void setStoryId(Long storyId) { this.storyId = storyId; }
}

