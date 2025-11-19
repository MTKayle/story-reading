package org.example.storyreading.userservice.listener;


import org.example.storyreading.userservice.config.RabbitMQConfig;
import org.example.storyreading.userservice.dto.PaymentEvent;
import org.example.storyreading.userservice.entity.UserEntity;
import org.example.storyreading.userservice.repository.UserRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component

public class PaymentEventListener {

    private final UserRepository userRepository;

    // Replace Lombok @Slf4j with explicit logger so compilation doesn't rely on Lombok annotation processing
    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    //consstructor
    public PaymentEventListener(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    @Transactional
    public void handlePaymentSuccessEvent(PaymentEvent event) {
        log.info("💰 ========== Payment event received ==========");
        log.info("💰 UserId: {}", event.getUserId());
        log.info("💰 TransactionId: {}", event.getTransactionId());
        log.info("💰 Amount: {}", event.getAmount());
        log.info("💰 PaymentType: {}", event.getPaymentType());
        log.info("💰 Status: {}", event.getStatus());
        log.info("💰 ============================================");

        // Validate event
        if (event == null) {
            log.error("❌ Event is null!");
            return;
        }

        if (event.getUserId() == null) {
            log.error("❌ UserId is null in event!");
            return;
        }

        if (event.getAmount() == null) {
            log.error("❌ Amount is null in event!");
            return;
        }

        if (event.getStatus() == null) {
            log.error("❌ Status is null in event!");
            return;
        }

        if (event.getPaymentType() == null) {
            log.error("❌ PaymentType is null in event!");
            return;
        }

        try {
            // Check if this is a DEPOSIT payment with SUCCESS status
            boolean isSuccess = "SUCCESS".equalsIgnoreCase(event.getStatus());
            boolean isDeposit = "DEPOSIT".equalsIgnoreCase(event.getPaymentType());
            
            log.info("💰 Event validation - isSuccess: {}, isDeposit: {}", isSuccess, isDeposit);

            if (isSuccess && isDeposit) {
                log.info("💰 Processing DEPOSIT payment for user: {}", event.getUserId());
                
                UserEntity user = userRepository.findById(event.getUserId())
                        .orElseThrow(() -> {
                            log.error("❌ User not found with id: {}", event.getUserId());
                            return new RuntimeException("User not found: " + event.getUserId());
                        });

                BigDecimal oldBalance = user.getBalance();
                log.info("💰 Current balance for user {}: {}", event.getUserId(), oldBalance);
                log.info("💰 Amount to add: {}", event.getAmount());
                
                BigDecimal newBalance = oldBalance.add(event.getAmount());
                user.setBalance(newBalance);

                userRepository.save(user);

                log.info("✅ Balance updated successfully for user {}: {} -> {}",
                        event.getUserId(), oldBalance, newBalance);
                
                // Verify the update
                UserEntity verifyUser = userRepository.findById(event.getUserId()).orElse(null);
                if (verifyUser != null) {
                    log.info("✅ Verification - Current balance in DB: {}", verifyUser.getBalance());
                }
            } else {
                log.info("⚠️ Skipping event - Status: '{}', PaymentType: '{}' (Expected: Status='SUCCESS', PaymentType='DEPOSIT')", 
                        event.getStatus(), event.getPaymentType());
            }
        } catch (Exception e) {
            log.error("❌ Failed to process payment event for user: {}", event.getUserId(), e);
            e.printStackTrace();
            throw e; // Để RabbitMQ retry
        }
    }
}
