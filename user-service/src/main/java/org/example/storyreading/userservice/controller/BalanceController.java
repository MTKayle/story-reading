package org.example.storyreading.userservice.controller;

import org.example.storyreading.userservice.dto.DeductBalanceRequest;
import org.example.storyreading.userservice.service.BalanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/balance")
public class BalanceController {

    private static final Logger log = LoggerFactory.getLogger(BalanceController.class);

    private final BalanceService balanceService;

    // Constructor thay thế cho @RequiredArgsConstructor
    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @PostMapping("/deduct")
    public ResponseEntity<?> deductBalance(@RequestBody DeductBalanceRequest request) {
        log.info("Received balance deduction request for userId: {}, amount: {}",
                request.getUserId(), request.getAmount());

        try {
            balanceService.deductBalance(
                request.getUserId(),
                request.getAmount(),
                request.getTransactionId()
            );
            return ResponseEntity.ok(Map.of("message", "Balance deducted successfully"));
        } catch (Exception e) {
            log.error("Failed to deduct balance: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
