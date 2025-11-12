package org.example.storyreading.commentservice.event.comment;

import org.example.storyreading.commentservice.config.RabbitMQConfig;
import org.example.storyreading.commentservice.event.rating.RatingDeletedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public CommentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishCommentEvent(CommentEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.COMMENT_EXCHANGE,
                RabbitMQConfig.COMMENT_ROUTING_KEY,
                event
        );
        System.out.println("Sent comment event: " + event.getContent());
        System.out.println("Sent comment event: " + event.getAuthorId());
    }

    public void publishCommentDeletedEvent(CommentDeletedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.COMMENT_EXCHANGE,
                RabbitMQConfig.COMMENT_DELETE_ROUTING_KEY, // tạo thêm routing key riêng
                event
        );
        System.out.println("🗑 Sent comment delete event: " + event.getCommentId());
    }
}


