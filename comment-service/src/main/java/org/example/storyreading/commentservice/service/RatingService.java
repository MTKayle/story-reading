package org.example.storyreading.commentservice.service;


import org.example.storyreading.commentservice.dto.rating.RatingRequest;
import org.example.storyreading.commentservice.dto.rating.RatingResponse;

public interface RatingService {
    RatingResponse rate(RatingRequest request); // thêm/sửa đánh giá
    void removeRating(Long userId, Long storyId); // hủy đánh giá
    void deleteRatingsByStoryId(Long storyId);
    RatingResponse getRating(Long storyId); // lấy average rating
    Integer getUserRating(Long userId, Long storyId); // lấy rating của user
}
