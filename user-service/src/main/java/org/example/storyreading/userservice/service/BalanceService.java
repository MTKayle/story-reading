package org.example.storyreading.userservice.service;

import org.example.storyreading.userservice.entity.UserEntity;
import org.example.storyreading.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class BalanceService {

    private static final Logger log = LoggerFactory.getLogger(BalanceService.class);

    private final UserRepository userRepository;

    // Constructor thay thế cho @RequiredArgsConstructor
    public BalanceService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void deductBalance(Long userId, BigDecimal amount, String transactionId) {
        log.info("Deducting balance for userId: {}, amount: {}, transactionId: {}",
                userId, amount, transactionId);

        // SELECT FOR UPDATE - pessimistic lock to prevent concurrent modifications
        UserEntity user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        BigDecimal currentBalance = user.getBalance();
        log.info("Current balance for userId {}: {}", userId, currentBalance);

        // Check if balance is sufficient
        if (currentBalance.compareTo(amount) < 0) {
            log.error("Insufficient balance. Current: {}, Required: {}", currentBalance, amount);
            throw new RuntimeException("Insufficient balance. Current: " + currentBalance + ", Required: " + amount);
        }

        // Deduct amount
        BigDecimal newBalance = currentBalance.subtract(amount);
        user.setBalance(newBalance);

        userRepository.save(user);
        log.info("Balance deducted successfully. New balance for userId {}: {}", userId, newBalance);
    }
}
