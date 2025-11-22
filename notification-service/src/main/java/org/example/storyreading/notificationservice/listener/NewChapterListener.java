package org.example.storyreading.notificationservice.listener;

import org.example.storyreading.notificationservice.config.RabbitMQConfig;
import org.example.storyreading.notificationservice.dto.chapter.NewChapterEvent;
import org.example.storyreading.notificationservice.service.imppl.NotificationServiceImpl;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NewChapterListener {

    private final NotificationServiceImpl notificationService;

    public NewChapterListener(NotificationServiceImpl notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.NEW_CHAPTER_QUEUE)
    public void handleNewChapterEvent(NewChapterEvent event) {
        try {
            System.out.println("📖 ========== New chapter event received ==========");
            System.out.println("📖 Story ID: " + event.getStoryId());
            System.out.println("📖 Story Title: " + event.getStoryTitle());
            System.out.println("📖 Chapter ID: " + event.getChapterId());
            System.out.println("📖 Chapter Number: " + event.getChapterNumber());
            System.out.println("📖 Chapter Title: " + event.getChapterTitle());
            System.out.println("📖 =================================================");

            // Validate event
            if (event == null) {
                System.err.println("❌ Event is null!");
                return;
            }

            if (event.getStoryId() == null) {
                System.err.println("❌ Story ID is null, rejecting message!");
                throw new AmqpRejectAndDontRequeueException("Invalid event: storyId is null");
            }

            notificationService.createNewChapterNotification(event);
            System.out.println("✅ New chapter notification processed successfully");
        } catch (Exception e) {
            System.err.println("❌ Failed to process new chapter event: " + e.getMessage());
            e.printStackTrace();
            throw new AmqpRejectAndDontRequeueException("Failed to process", e);
        }
    }
}

