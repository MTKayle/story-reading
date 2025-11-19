package org.example.storyreading.notificationservice.dto.payment;

import java.io.Serializable;
import java.math.BigDecimal;

public class PaymentNotificationEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String transactionId;
    private BigDecimal amount;
    private String status; // SUCCESS, FAILED
    private String paymentType; // DEPOSIT, PURCHASE
    private String message;

    public PaymentNotificationEvent() {
    }

    public PaymentNotificationEvent(Long userId, String transactionId, BigDecimal amount, String status, String paymentType, String message) {
        this.userId = userId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.status = status;
        this.paymentType = paymentType;
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

