package org.example.storyreading.paymentservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.storyreading.paymentservice.dto.DepositRequest;
import org.example.storyreading.paymentservice.dto.PurchaseStoryRequest;
import org.example.storyreading.paymentservice.dto.VNPayResponse;
import org.example.storyreading.paymentservice.entity.Payment;
import org.example.storyreading.paymentservice.service.PaymentService;
import org.example.storyreading.paymentservice.service.StoryPurchaseService;
import org.springframework.beans.factory.annotation.Value;
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
    private final String frontendBaseUrl;

    // Explicit constructor to initialize final field (avoids Lombok dependency)
    public PaymentController(
            PaymentService paymentService,
            StoryPurchaseService storyPurchaseService,
            @Value("${frontend.base-url:http://localhost:3000}") String frontendBaseUrl
    ) {
        this.paymentService = paymentService;
        this.storyPurchaseService = storyPurchaseService;
        this.frontendBaseUrl = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
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

        // Validate userId
        if (userId == null || userId <= 0) {
            log.error("Invalid userId: {}", userId);
            return ResponseEntity.badRequest().body(Map.of("error", "User ID không hợp lệ"));
        }

        try {
            Payment payment = storyPurchaseService.purchaseStory(userId, request);
            return ResponseEntity.ok(payment);
        } catch (IllegalArgumentException e) {
            log.error("Story purchase validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Story purchase failed: {}", e.getMessage(), e);
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

        // Redirect to frontend homepage with result
        String responseCode = vnpParams.get("vnp_ResponseCode");
        String txnRef = vnpParams.get("vnp_TxnRef");

        if ("00".equals(responseCode)) {
            // Payment success - redirect to homepage with success status
            String successUrl = frontendBaseUrl + "/?paymentStatus=success"
                    + (txnRef != null ? "&txnRef=" + txnRef : "");
            log.info("Redirecting to homepage with success status: {}", successUrl);
            RedirectView redirectView = new RedirectView(successUrl);
            redirectView.setContextRelative(false);
            return redirectView;
        }

        // Payment failed/cancelled - redirect to homepage with failure status
        String failureUrl = frontendBaseUrl + "/?paymentStatus=failed"
                + (txnRef != null ? "&txnRef=" + txnRef : "");
        log.info("Redirecting to homepage with failure status: {}", failureUrl);
        RedirectView redirectView = new RedirectView(failureUrl);
        redirectView.setContextRelative(false);
        return redirectView;
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<?> getPaymentByTransaction(@PathVariable String transactionId) {
        return paymentService.getPaymentByTransactionId(transactionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/history")
    public ResponseEntity<List<Payment>> getUserPaymentHistory(
            @RequestHeader("X-User-Id") Long userId) {
        List<Payment> payments = paymentService.getUserPayments(userId);
        return ResponseEntity.ok(payments);
    }
}
