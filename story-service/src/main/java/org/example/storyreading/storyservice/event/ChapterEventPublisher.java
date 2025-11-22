package org.example.storyreading.storyservice.event;

import org.example.storyreading.storyservice.config.RabbitMQConfig;
import org.example.storyreading.storyservice.dto.NewChapterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChapterEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ChapterEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public ChapterEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishNewChapterEvent(NewChapterEvent event) {
        try {
            log.info("📤 Publishing new chapter event: storyId={}, chapterNumber={}", event.getStoryId(), event.getChapterNumber());
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NEW_CHAPTER_EXCHANGE,
                    RabbitMQConfig.NEW_CHAPTER_ROUTING_KEY,
                    event
            );
            log.info("✅ New chapter event published successfully");
        } catch (Exception e) {
            log.error("❌ Failed to publish new chapter event", e);
            // Don't throw - we don't want to fail chapter creation if notification fails
        }
    }
}

