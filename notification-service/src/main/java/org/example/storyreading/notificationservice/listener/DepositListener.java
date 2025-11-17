package org.example.storyreading.notificationservice.listener;

import org.example.storyreading.notificationservice.config.RabbitMQConfig;
import org.example.storyreading.notificationservice.dto.deposit.DepositEvent;
import org.example.storyreading.notificationservice.service.imppl.NotificationServiceImpl;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DepositListener {

    private final NotificationServiceImpl notificationService;

    public DepositListener(NotificationServiceImpl notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.DEPOSIT_QUEUE)
    public void handleDepositEvent(DepositEvent event) {
        System.out.println("💰 Deposit success: " + event.getAmount() + " for userId=" + event.getUserId());
        notificationService.createDepositNotification(event);
    }
}
