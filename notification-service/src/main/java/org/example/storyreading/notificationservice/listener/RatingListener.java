package org.example.storyreading.notificationservice.listener;

import org.example.storyreading.notificationservice.config.RabbitMQConfig;
import org.example.storyreading.notificationservice.dto.rating.RatingDeletedEvent;
import org.example.storyreading.notificationservice.dto.rating.RatingEvent;
import org.example.storyreading.notificationservice.service.imppl.NotificationServiceImpl;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RatingListener {

    private final NotificationServiceImpl notificationService;

    public RatingListener(NotificationServiceImpl notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.RATING_QUEUE)
    public void handleRatingEvent(RatingEvent event) {
        System.out.println("⭐ New rating: " + event.getStars() + " stars");
        notificationService.createRatingNotification(event);
    }
    @RabbitListener(queues = RabbitMQConfig.RATING_DELETE_QUEUE)
    public void handleRatingDeletedEvent(RatingDeletedEvent event) {
        System.out.println("🗑 Rating deleted for storyId=" + event.getRatingId());
        notificationService.softDeleteByTypeId(event.getRatingId());
    }
}

