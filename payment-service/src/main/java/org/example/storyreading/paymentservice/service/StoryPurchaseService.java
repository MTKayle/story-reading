package org.example.storyreading.paymentservice.service;

import org.example.storyreading.paymentservice.client.UserServiceClient;
import org.example.storyreading.paymentservice.config.RabbitMQConfig;
import org.example.storyreading.paymentservice.dto.PurchaseStoryRequest;
import org.example.storyreading.paymentservice.dto.StoryPurchaseEvent;
import org.example.storyreading.paymentservice.entity.Payment;
import org.example.storyreading.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class StoryPurchaseService {

    private static final Logger log = LoggerFactory.getLogger(StoryPurchaseService.class);

    private final PaymentRepository paymentRepository;
    private final UserServiceClient userServiceClient;
    private final RabbitTemplate rabbitTemplate;

    // Constructor thay thế cho @RequiredArgsConstructor
    public StoryPurchaseService(PaymentRepository paymentRepository,
                                UserServiceClient userServiceClient,
                                RabbitTemplate rabbitTemplate) {
        this.paymentRepository = paymentRepository;
        this.userServiceClient = userServiceClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public Payment purchaseStory(Long userId, PurchaseStoryRequest request) {
        log.info("Starting story purchase for userId: {}, storyId: {}", userId, request.getStoryId());

        // Kiểm tra xem user đã mua truyện này chưa
        boolean alreadyPurchased = paymentRepository.existsByUserIdAndStoryIdAndPaymentTypeAndStatus(
            userId,
            request.getStoryId(),
            Payment.PaymentType.PURCHASE,
            Payment.PaymentStatus.SUCCESS
        );

        if (alreadyPurchased) {
            log.warn("User {} already purchased story {}", userId, request.getStoryId());
            throw new RuntimeException("Bạn đã mua truyện này rồi");
        }

        // Generate unique transaction ID
        String transactionId = "PURCHASE-" + UUID.randomUUID().toString();

        // Create pending payment record
        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setStoryId(request.getStoryId());
        payment.setTransactionId(transactionId);
        payment.setAmount(request.getPrice());
        payment.setPaymentType(Payment.PaymentType.PURCHASE);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setDescription("Purchase story ID: " + request.getStoryId());

        payment = paymentRepository.save(payment);
        log.info("Created pending payment with ID: {}", payment.getId());

        try {
            // Call user-service to check balance and deduct amount with SELECT FOR UPDATE
            boolean deductSuccess = userServiceClient.checkAndDeductBalance(
                userId,
                request.getPrice(),
                transactionId
            );

            if (!deductSuccess) {
                log.error("Failed to deduct balance for userId: {}", userId);
                payment.setStatus(Payment.PaymentStatus.FAILED);
                payment.setDescription("Insufficient balance or deduction failed");
                paymentRepository.save(payment);
                throw new RuntimeException("Insufficient balance or deduction failed");
            }

            log.info("Balance deducted successfully for userId: {}", userId);

            // Update payment status to success
            payment.setStatus(Payment.PaymentStatus.SUCCESS);
            payment = paymentRepository.save(payment);

            // Publish event to RabbitMQ for story-service to grant access
            StoryPurchaseEvent event = new StoryPurchaseEvent(
                userId,
                request.getStoryId(),
                request.getPrice(),
                transactionId
            );

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.STORY_PURCHASE_EXCHANGE,
                RabbitMQConfig.STORY_PURCHASE_ROUTING_KEY,
                event
            );

            log.info("Published story purchase event to RabbitMQ for userId: {}, storyId: {}",
                userId, request.getStoryId());

            return payment;

        } catch (Exception e) {
            log.error("Error during story purchase: {}", e.getMessage(), e);
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setDescription("Error: " + e.getMessage());
            paymentRepository.save(payment);
            throw new RuntimeException("Story purchase failed: " + e.getMessage());
        }
    }
}
