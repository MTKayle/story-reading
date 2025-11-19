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
            System.out.println("💬 ========== New comment event received ==========");
            System.out.println("💬 Event class: " + event.getClass().getName());
            System.out.println("💬 Comment ID: " + event.getCommentId());
            System.out.println("💬 Content: " + event.getContent());
            System.out.println("💬 User ID (sender): " + event.getUserId());
            System.out.println("💬 Author ID (story author): " + event.getAuthorId());
            System.out.println("💬 Parent ID: " + event.getParentId());
            System.out.println("💬 Parent User ID: " + event.getParentUserId());
            System.out.println("💬 Story ID: " + event.getStoryId());
            System.out.println("💬 =================================================");
            
            // Validate event không null
            if (event == null) {
                System.err.println("❌ Event is null!");
                return;
            }

            // Validate: cần có userId và storyId
            if (event.getUserId() == null) {
                System.err.println("❌ User ID is null, rejecting message!");
                throw new AmqpRejectAndDontRequeueException("Invalid event: userId is null");
            }
            
            if (event.getStoryId() == null) {
                System.err.println("❌ Story ID is null, rejecting message!");
                throw new AmqpRejectAndDontRequeueException("Invalid event: storyId is null");
            }

            // authorId có thể null nếu không phải comment cho tác giả truyện
            // parentUserId có thể null nếu không phải reply
            notificationService.createCommentNotification(event);
            System.out.println("✅ Comment notification processed successfully");
        } catch (Exception e) {
            System.err.println("❌ Failed to process comment event: " + e.getMessage());
            e.printStackTrace();
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
