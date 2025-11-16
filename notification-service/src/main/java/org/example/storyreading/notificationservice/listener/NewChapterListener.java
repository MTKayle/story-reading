package org.example.storyreading.notificationservice.listener;

import org.example.storyreading.notificationservice.config.RabbitMQConfig;
import org.example.storyreading.notificationservice.dto.story.NewChapterEvent;
import org.example.storyreading.notificationservice.service.imppl.NotificationServiceImpl;
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
        System.out.println("📚 New chapter: " + event.getChapterTitle() + " for story: " + event.getStoryTitle());
        notificationService.createNewChapterNotification(event);
    }
}
