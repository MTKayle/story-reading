package org.example.storyreading.notificationservice.listener;

import org.example.storyreading.notificationservice.config.RabbitMQConfig;
import org.example.storyreading.notificationservice.dto.payment.PaymentNotificationEvent;
import org.example.storyreading.notificationservice.entity.Notification;
import org.example.storyreading.notificationservice.repository.NotificationRepository;
import org.example.storyreading.notificationservice.service.imppl.NotificationServiceImpl;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentNotificationListener {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public PaymentNotificationListener(NotificationRepository notificationRepository, SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_NOTIFICATION_QUEUE)
    public void handlePaymentNotificationEvent(PaymentNotificationEvent event) {
        try {
            System.out.println("💳 ========== Payment notification event received ==========");
            System.out.println("💳 UserId: " + event.getUserId());
            System.out.println("💳 TransactionId: " + event.getTransactionId());
            System.out.println("💳 Amount: " + event.getAmount());
            System.out.println("💳 Status: " + event.getStatus());
            System.out.println("💳 PaymentType: " + event.getPaymentType());
            System.out.println("💳 Message: " + event.getMessage());
            System.out.println("💳 ==========================================================");

            if (event.getUserId() == null) {
                System.err.println("❌ UserId is null, skipping notification");
                return;
            }

            // Create notification
            // Extract numeric part from transactionId for typeId (e.g., "DEPOSIT_1234567890_1" -> 1234567890)
            Long typeId = null;
            if (event.getTransactionId() != null && !event.getTransactionId().isEmpty()) {
                try {
                    // Try to extract numbers from transactionId
                    String numericPart = event.getTransactionId().replaceAll("[^0-9]", "");
                    if (!numericPart.isEmpty()) {
                        // Take last 10 digits to avoid overflow
                        String lastDigits = numericPart.length() > 10 ? numericPart.substring(numericPart.length() - 10) : numericPart;
                        typeId = Long.parseLong(lastDigits);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("⚠️ Could not parse transactionId to Long, using null for typeId");
                }
            }

            Notification notification = Notification.builder()
                    .recipientId(event.getUserId())
                    .senderId(event.getUserId()) // Payment is from the user themselves
                    .content(event.getMessage())
                    .link("/") // Link to homepage
                    .typeId(typeId)
                    .isDeleted(false)
                    .isRead(false)
                    .build();

            Notification saved = notificationRepository.save(notification);
            System.out.println("✅ Payment notification saved with ID: " + saved.getId());

            // Send via WebSocket
            messagingTemplate.convertAndSend("/topic/notifications/" + event.getUserId(), saved);
            System.out.println("✅ Payment notification sent via WebSocket to user: " + event.getUserId());

        } catch (Exception e) {
            System.err.println("❌ Failed to process payment notification event: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

