package org.example.storyreading.commentservice.event.rating;

import java.io.Serial;
import java.io.Serializable;

public class RatingEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long ratingId;
    private Long storyId;
    private Long userId;     // người đánh giá
    private int stars;       // số sao
    private Long authorId;   // tác giả truyện

    public RatingEvent() {}

    public RatingEvent(Long ratingId, Long storyId, Long userId, int stars, Long authorId) {
        this.ratingId = ratingId;
        this.storyId = storyId;
        this.userId = userId;
        this.stars = stars;
        this.authorId = authorId;
    }

    // getters & setters
    public Long getRatingId() { return ratingId; }
    public void setRatingId(Long ratingId) { this.ratingId = ratingId; }
    public Long getStoryId() { return storyId; }
    public void setStoryId(Long storyId) { this.storyId = storyId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
}

