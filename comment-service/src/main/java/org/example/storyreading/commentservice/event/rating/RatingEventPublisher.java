package org.example.storyreading.commentservice.event.rating;

import org.example.storyreading.commentservice.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RatingEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RatingEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishRatingEvent(RatingEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.RATING_EXCHANGE,
                RabbitMQConfig.RATING_ROUTING_KEY,
                event
        );
        System.out.println("Sent rating event: " + event.getStars() + "⭐ for storyId=" + event.getStoryId());
    }

    public void publishRatingDeletedEvent(RatingDeletedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.RATING_EXCHANGE,
                RabbitMQConfig.RATING_DELETE_ROUTING_KEY, // tạo thêm routing key riêng
                event
        );
        System.out.println("🗑 Sent comment delete event: " + event.getRatingId());
    }
}
