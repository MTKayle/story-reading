package org.example.storyreading.storyservice.listener;

import org.example.storyreading.storyservice.config.RabbitMQConfig;
import org.example.storyreading.storyservice.dto.StoryPurchaseEvent;
import org.example.storyreading.storyservice.service.impl.PurchaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class StoryPurchaseListener {

    private static final Logger log = LoggerFactory.getLogger(StoryPurchaseListener.class);

    private final PurchaseService purchaseService;

    // Constructor thay thế cho @RequiredArgsConstructor
    public StoryPurchaseListener(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @RabbitListener(queues = RabbitMQConfig.STORY_PURCHASE_QUEUE)
    public void handleStoryPurchase(StoryPurchaseEvent event) {
        log.info("Received story purchase event: userId={}, storyId={}, transactionId={}",
                event.getUserId(), event.getStoryId(), event.getTransactionId());

        try {
            purchaseService.grantAccess(event.getUserId(), event.getStoryId());
            log.info("Successfully granted access for userId={}, storyId={}",
                    event.getUserId(), event.getStoryId());
        } catch (Exception e) {
            log.error("Failed to grant access: {}", e.getMessage(), e);
            // In production, you might want to implement a dead letter queue
            // or retry mechanism here
        }
    }
}
