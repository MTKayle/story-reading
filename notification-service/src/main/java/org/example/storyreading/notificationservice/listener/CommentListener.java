package org.example.storyreading.notificationservice.listener;

import org.example.storyreading.notificationservice.config.RabbitMQConfig;
import org.example.storyreading.notificationservice.dto.comment.CommentDeletedEvent;
import org.example.storyreading.notificationservice.dto.comment.CommentEvent;
import org.example.storyreading.notificationservice.service.imppl.NotificationServiceImpl;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class CommentListener {

    private final NotificationServiceImpl notificationService;

    public CommentListener(NotificationServiceImpl notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.COMMENT_QUEUE)
    public void handleCommentEvent(CommentEvent event) {
        try {
            System.out.println("💬 New comment received: " + event.getContent());
            System.out.println("👤 Author ID: " + event.getAuthorId());

            // Validate trước khi xử lý
            if (event.getAuthorId() == null) {
                System.err.println("❌ Author ID is null, rejecting message!");
                throw new AmqpRejectAndDontRequeueException("Invalid event: authorId is null");
            }

            notificationService.createCommentNotification(event);
        } catch (Exception e) {
            System.err.println("❌ Failed to process comment event: " + e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Failed to process", e);
        }
    }
    @RabbitListener(queues = RabbitMQConfig.COMMENT_DELETE_QUEUE)
    public void handleCommentDeleted(CommentDeletedEvent event) {
        System.out.println("🗑 Received delete event for commentId = " + event.getCommentId());
        notificationService.softDeleteByTypeId(event.getCommentId());
        if (event.getReactionIds() != null) {
            for (Long reactionId : event.getReactionIds()) {
                System.out.println("🗑 Also deleting notification for associated reactionId = " + reactionId);
                notificationService.softDeleteByTypeId(reactionId);
            }
        }
    }
}
