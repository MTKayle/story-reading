package org.example.storyreading.notificationservice.dto.rating;

import java.io.Serial;
import java.io.Serializable;

public class RatingDeletedEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long ratingId;

    public RatingDeletedEvent() {}

    public RatingDeletedEvent(Long ratingId) {
        this.ratingId = ratingId;
    }

    public Long getRatingId() {
        return ratingId;
    }

    public void setRatingId(Long ratingId) {
        this.ratingId = ratingId;
    }
}