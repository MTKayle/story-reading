package org.example.storyreading.notificationservice.listener;

import org.example.storyreading.notificationservice.config.RabbitMQConfig;
import org.example.storyreading.notificationservice.dto.payment.PaymentEvent;
import org.example.storyreading.notificationservice.service.imppl.NotificationServiceImpl;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentListener {

    private final NotificationServiceImpl notificationService;

    public PaymentListener(NotificationServiceImpl notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    public void handlePurchaseStoryEvent(PaymentEvent event) {
        System.out.println("📖 Purchase story: " + event.getStoryTitle() + " by userId=" + event.getUserId());
        notificationService.createPurchaseStoryNotification(event);
    }
}
