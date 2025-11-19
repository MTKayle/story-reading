package org.example.storyreading.commentservice.event.comment;

import java.io.Serial;
import java.io.Serializable;

public class CommentEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long commentId;
    private String content;
    private Long userId;     // người tạo comment
    private Long parentId;   // nếu là reply thì parent comment id
    private Long parentUserId; // userId của người được reply (nếu là reply)
    private Long storyId;    // id truyện
    private Long authorId;   // tác giả truyện


    public CommentEvent() {}

    public CommentEvent(Long commentId, String content, Long userId, Long parentId, Long parentUserId, Long storyId, Long authorId) {
        this.commentId = commentId;
        this.content = content;
        this.userId = userId;
        this.parentId = parentId;
        this.parentUserId = parentUserId;
        this.storyId = storyId;
        this.authorId = authorId;
    }

    // getters & setters
    public Long getCommentId() { return commentId; }
    public void setCommentId(Long commentId) { this.commentId = commentId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public Long getParentUserId() { return parentUserId; }
    public void setParentUserId(Long parentUserId) { this.parentUserId = parentUserId; }

    public Long getStoryId() { return storyId; }
    public void setStoryId(Long storyId) { this.storyId = storyId; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
}
