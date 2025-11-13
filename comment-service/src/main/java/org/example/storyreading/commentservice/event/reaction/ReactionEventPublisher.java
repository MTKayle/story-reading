package org.example.storyreading.commentservice.event.reaction;

import org.example.storyreading.commentservice.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReactionEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ReactionEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishReactionEvent(ReactionEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.REACTION_EXCHANGE,
                RabbitMQConfig.REACTION_ROUTING_KEY,
                event
        );
        System.out.println("Sent reaction event: " + event.getType() + " on commentId=" + event.getCommentId());
    }

    public void publishReactionDeletedEvent(ReactionDeletedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.REACTION_EXCHANGE,
                RabbitMQConfig.REACTION_DELETE_ROUTING_KEY, // tạo thêm routing key riêng
                event
        );
        System.out.println("🗑 Sent reaction delete event: " + event.getReactionId());
    }
}

