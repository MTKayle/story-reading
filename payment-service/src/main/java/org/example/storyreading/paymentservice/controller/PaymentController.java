package org.example.storyreading.paymentservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.storyreading.paymentservice.dto.DepositRequest;
import org.example.storyreading.paymentservice.dto.PurchaseStoryRequest;
import org.example.storyreading.paymentservice.dto.VNPayResponse;
import org.example.storyreading.paymentservice.entity.Payment;
import org.example.storyreading.paymentservice.service.PaymentService;
import org.example.storyreading.paymentservice.service.StoryPurchaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    // Explicit logger in case Lombok @Slf4j isn't processed in the build environment
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final StoryPurchaseService storyPurchaseService;

    // Explicit constructor to initialize final field (avoids Lombok dependency)
    public PaymentController(PaymentService paymentService, StoryPurchaseService storyPurchaseService) {
        this.paymentService = paymentService;
        this.storyPurchaseService = storyPurchaseService;
    }

    @PostMapping(value = "/deposit",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VNPayResponse> createDeposit(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody DepositRequest request) {

        log.info("Deposit request from user: {}, amount: {}", userId, request.getAmount());
        VNPayResponse response = paymentService.createDepositPayment(userId, request);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @PostMapping(value = "/purchase-story",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> purchaseStory(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PurchaseStoryRequest request) {

        log.info("Story purchase request from user: {}, storyId: {}, price: {}",
                userId, request.getStoryId(), request.getPrice());

        try {
            Payment payment = storyPurchaseService.purchaseStory(userId, request);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            log.error("Story purchase failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/vnpay/callback")
    public RedirectView vnpayCallback(HttpServletRequest request) {
        Map<String, String> vnpParams = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values.length > 0) {
                vnpParams.put(key, values[0]);
            }
        });

        log.info("VNPay callback received with params: {}", vnpParams.keySet());

        paymentService.handleVNPayCallback(vnpParams);

        // Redirect to frontend with result
        String responseCode = vnpParams.get("vnp_ResponseCode");
        String txnRef = vnpParams.get("vnp_TxnRef");

        if ("00".equals(responseCode)) {
            return new RedirectView("http://localhost:3000/payment/success?txnRef=" + txnRef);
        } else {
            return new RedirectView("http://localhost:3000/payment/failed?txnRef=" + txnRef);
        }
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<?> getPaymentByTransaction(@PathVariable String transactionId) {
        return paymentService.getPaymentByTransactionId(transactionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Kiểm tra xem user đã mua truyện này chưa
     * GET /api/payment/check-purchase/{storyId}
     * Header: X-User-Id
     * Response: { "purchased": true/false }
     */
    @GetMapping("/check-purchase/{storyId}")
    public ResponseEntity<Map<String, Boolean>> checkPurchaseStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long storyId) {

        log.info("Checking purchase status for userId: {}, storyId: {}", userId, storyId);

        boolean hasPurchased = storyPurchaseService.checkPurchaseStatus(userId, storyId);

        return ResponseEntity.ok(Map.of("purchased", hasPurchased));
    }

    @GetMapping("/user/payments")
    public ResponseEntity<List<Payment>> getUserPayments(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(paymentService.getUserPayments(userId));
    }
}
