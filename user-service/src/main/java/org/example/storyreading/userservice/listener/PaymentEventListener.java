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
        log.info("Received payment event: userId={}, transactionId={}, amount={}, type={}",
                event.getUserId(), event.getTransactionId(), event.getAmount(), event.getPaymentType());

        try {
            if ("SUCCESS".equals(event.getStatus()) && "DEPOSIT".equals(event.getPaymentType())) {
                UserEntity user = userRepository.findById(event.getUserId())
                        .orElseThrow(() -> new RuntimeException("User not found: " + event.getUserId()));

                BigDecimal oldBalance = user.getBalance();
                BigDecimal newBalance = oldBalance.add(event.getAmount());
                user.setBalance(newBalance);

                userRepository.save(user);

                log.info("Updated balance for user {}: {} -> {}",
                        event.getUserId(), oldBalance, newBalance);
            }
        } catch (Exception e) {
            log.error("Failed to process payment event for user: {}", event.getUserId(), e);
            throw e; // Để RabbitMQ retry
        }
    }
}
