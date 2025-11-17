package org.example.storyreading.paymentservice.service;

import org.example.storyreading.paymentservice.config.RabbitMQConfig;
import org.example.storyreading.paymentservice.config.VNPayConfig;
import org.example.storyreading.paymentservice.dto.DepositEvent;
import org.example.storyreading.paymentservice.dto.DepositRequest;
import org.example.storyreading.paymentservice.dto.PaymentEvent;
import org.example.storyreading.paymentservice.dto.VNPayResponse;
import org.example.storyreading.paymentservice.entity.Payment;
import org.example.storyreading.paymentservice.repository.PaymentRepository;
import org.example.storyreading.paymentservice.util.VNPayUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final VNPayConfig vnPayConfig;
    private final RabbitTemplate rabbitTemplate;

    // Explicit constructor to avoid Lombok dependency
    public PaymentService(PaymentRepository paymentRepository, VNPayConfig vnPayConfig, RabbitTemplate rabbitTemplate) {
        this.paymentRepository = paymentRepository;
        this.vnPayConfig = vnPayConfig;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public VNPayResponse createDepositPayment(Long userId, DepositRequest request) {
        // Tạo transaction ID unique
        String transactionId = "DEPOSIT_" + System.currentTimeMillis() + "_" + userId;
        String vnpayTxnRef = VNPayUtil.getRandomNumber(8);

        // Tạo payment record với status PENDING
        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setTransactionId(transactionId);
        payment.setVnpayTxnRef(vnpayTxnRef);
        payment.setAmount(request.getAmount());
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setPaymentType(Payment.PaymentType.DEPOSIT);
        payment.setDescription(request.getDescription() != null ? request.getDescription() : "Nạp tiền vào tài khoản");

        paymentRepository.save(payment);
        log.info("Created payment record: {}", transactionId);

        // Tạo VNPay payment URL
        String paymentUrl = createVNPayUrl(payment);

        return new VNPayResponse(paymentUrl, transactionId, "Payment URL created successfully");
    }

    private String createVNPayUrl(Payment payment) {
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());

        // Convert amount to VND cents (x100)
        long amountInCents = payment.getAmount().multiply(new BigDecimal(100)).longValue();
        vnpParams.put("vnp_Amount", String.valueOf(amountInCents));

        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", payment.getVnpayTxnRef());
        vnpParams.put("vnp_OrderInfo", payment.getDescription());
        vnpParams.put("vnp_OrderType", vnPayConfig.getOrderType());
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnpExpireDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        vnpParams.put("vnp_IpAddr", "127.0.0.1");

        // NOTE: Do NOT include vnp_SecureHashType in the data used for HMAC calculation.
        // Build canonical data string for hashing and compute HMAC
        String hashData = VNPayUtil.buildHashData(vnpParams);
        String secureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);

        // Log for debugging signature issues
        log.info("VNPay hashData=\n{}", hashData);
        log.info("VNPay secureHash={}", secureHash);

        // Put secure hash param and secure hash type after computing HMAC
        vnpParams.put("vnp_SecureHash", secureHash);
        vnpParams.put("vnp_SecureHashType", "SHA512");

        String paymentUrl = VNPayUtil.getPaymentURL(vnpParams, vnPayConfig.getUrl());
        log.info("VNPay paymentUrl={}", paymentUrl);

        return paymentUrl;
    }

    @Transactional
    public void handleVNPayCallback(Map<String, String> vnpParams) {
        String vnpTxnRef = vnpParams.get("vnp_TxnRef");
        String vnpResponseCode = vnpParams.get("vnp_ResponseCode");
        String vnpTransactionNo = vnpParams.get("vnp_TransactionNo");
        String vnpSecureHash = vnpParams.get("vnp_SecureHash");

        log.info("VNPay callback received for txnRef: {}, responseCode: {}", vnpTxnRef, vnpResponseCode);

        // Verify secure hash
        Map<String, String> fields = new HashMap<>(vnpParams);
        fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        String calculatedHash = VNPayUtil.hashAllFields(fields, vnPayConfig.getHashSecret());

        if (!calculatedHash.equals(vnpSecureHash)) {
            log.error("Invalid secure hash for txnRef: {}", vnpTxnRef);
            return;
        }

        // Find payment
        Optional<Payment> paymentOpt = paymentRepository.findByVnpayTxnRef(vnpTxnRef);
        if (paymentOpt.isEmpty()) {
            log.error("Payment not found for vnpTxnRef: {}", vnpTxnRef);
            return;
        }

        Payment payment = paymentOpt.get();
        payment.setVnpayResponseCode(vnpResponseCode);
        payment.setVnpayTransactionNo(vnpTransactionNo);

        // Check response code
        if ("00".equals(vnpResponseCode)) {
            // Payment success
            payment.setStatus(Payment.PaymentStatus.SUCCESS);
            paymentRepository.save(payment);
            log.info("Payment success for txnRef: {}", vnpTxnRef);

            // Send event to RabbitMQ for notification
            if (payment.getPaymentType() == Payment.PaymentType.DEPOSIT) {
                sendDepositSuccessEvent(payment);
            }
        } else {
            // Payment failed
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.warn("Payment failed for txnRef: {}, responseCode: {}", vnpTxnRef, vnpResponseCode);
        }
    }

    private void sendDepositSuccessEvent(Payment payment) {
        try {
            DepositEvent event = new DepositEvent(
                payment.getUserId(),
                payment.getAmount().longValue(),
                payment.getId()
            );

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.DEPOSIT_EXCHANGE,
                RabbitMQConfig.DEPOSIT_ROUTING_KEY,
                event
            );

            log.info("💰 Deposit success event sent to RabbitMQ for userId: {}, amount: {}",
                payment.getUserId(), payment.getAmount());
        } catch (Exception e) {
            log.error("Failed to send deposit event to RabbitMQ", e);
        }
    }

    public Optional<Payment> getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId);
    }

    public List<Payment> getUserPayments(Long userId) {
        return paymentRepository.findByUserId(userId);
    }
}
