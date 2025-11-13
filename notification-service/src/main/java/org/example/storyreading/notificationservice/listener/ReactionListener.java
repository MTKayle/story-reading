package org.example.storyreading.notificationservice.listener;

import org.example.storyreading.notificationservice.config.RabbitMQConfig;
import org.example.storyreading.notificationservice.dto.reaction.ReactionDeletedEvent;
import org.example.storyreading.notificationservice.dto.reaction.ReactionEvent;
import org.example.storyreading.notificationservice.service.imppl.NotificationServiceImpl;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReactionListener {

    private final NotificationServiceImpl notificationService;

    public ReactionListener(NotificationServiceImpl notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.REACTION_QUEUE)
    public void handleReactionEvent(ReactionEvent event) {
        System.out.println("👍 Reaction received: " + event.getType());
        notificationService.createReactionNotification(event);
    }
    @RabbitListener(queues = RabbitMQConfig.REACTION_DELETE_QUEUE)
    public void handleReactionDeletedEvent(ReactionDeletedEvent event) {
        System.out.println("🗑 Reaction delete received for reactionId: " + event.getReactionId());
        notificationService.softDeleteByTypeId(event.getReactionId());
    }
}

