package org.example.storyreading.paymentservice.dto;

import java.math.BigDecimal;

public class DeductBalanceRequest {
    private Long userId;
    private BigDecimal amount;
    private String transactionId;

    // No-args constructor
    public DeductBalanceRequest() {
    }

    // All-args constructor
    public DeductBalanceRequest(Long userId, BigDecimal amount, String transactionId) {
        this.userId = userId;
        this.amount = amount;
        this.transactionId = transactionId;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}
