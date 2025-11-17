package org.example.storyreading.commentservice.service.impl;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.storyreading.commentservice.dto.rating.RatingRequest;
import org.example.storyreading.commentservice.dto.rating.RatingResponse;
import org.example.storyreading.commentservice.entity.Rating;
import org.example.storyreading.commentservice.event.rating.RatingDeletedEvent;
import org.example.storyreading.commentservice.event.rating.RatingEventPublisher;
import org.example.storyreading.commentservice.event.rating.RatingEvent;
import org.example.storyreading.commentservice.repository.RatingRepository;
import org.example.storyreading.commentservice.service.RatingService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RatingEventPublisher ratingEventPublisher;

    @Override
    public RatingResponse rate(RatingRequest request) {
        if(request.getStars() < 1 || request.getStars() > 5)
            throw new IllegalArgumentException("Stars must be 1-5");

        Rating rating = ratingRepository.findByUserIdAndStoryId(request.getUserId(), request.getStoryId())
                .orElse(Rating.builder()
                        .userId(request.getUserId())
                        .storyId(request.getStoryId())
                        .build());

        rating.setStars(request.getStars());
        ratingRepository.save(rating);

        ratingEventPublisher.publishRatingEvent(
                new RatingEvent(rating.getId(), rating.getStoryId(), rating.getUserId(),
                        rating.getStars(), request.getStoryAuthorId())
        );


        Double avg = ratingRepository.getAverageRating(request.getStoryId());

        RatingResponse response = RatingResponse.builder()
                .storyId(request.getStoryId())
                .averageStars(avg)
                .build();

        messagingTemplate.convertAndSend("/topic/stories/rating/" + request.getStoryId(), response);
        return response;
    }

    @Override
    @Transactional
    public void removeRating(Long userId, Long storyId) {
        // 1️⃣ lấy rating trước khi xóa
        Rating rating = ratingRepository.findByUserIdAndStoryId(userId, storyId)
                .orElseThrow(() -> new RuntimeException("Rating not found"));

        Long ratingId = rating.getId();

        // 2️⃣ xóa rating
        ratingRepository.delete(rating);

        // 3️⃣ tính lại trung bình
        Double avg = ratingRepository.getAverageRating(storyId);

        RatingResponse response = RatingResponse.builder()
                .storyId(storyId)
                .averageStars(avg)
                .build();

        // 4️⃣ gửi realtime
        messagingTemplate.convertAndSend("/topic/stories/rating/" + storyId, response);

        // 5️⃣ phát sự kiện sang notification-service
        ratingEventPublisher.publishRatingDeletedEvent(
                new RatingDeletedEvent(ratingId)
        );
    }


    @Override
    @Transactional
    public void deleteRatingsByStoryId(Long storyId) {
        List<Long> ratingsId = ratingRepository.findIdsByStoryId(storyId);
        for (Long ratingId : ratingsId) {
            // 5️⃣ phát sự kiện sang notification-service
            ratingEventPublisher.publishRatingDeletedEvent(
                    new RatingDeletedEvent(ratingId)
            );
        }
        ratingRepository.deleteByStoryId(storyId);
    }
}

