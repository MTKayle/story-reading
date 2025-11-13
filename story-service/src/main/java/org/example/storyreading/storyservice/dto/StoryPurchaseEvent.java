package org.example.storyreading.storyservice.dto;

import java.math.BigDecimal;

public class StoryPurchaseEvent {
    private Long userId;
    private Long storyId;
    private BigDecimal amount;
    private String transactionId;

    // No-args constructor
    public StoryPurchaseEvent() {
    }

    // All-args constructor
    public StoryPurchaseEvent(Long userId, Long storyId, BigDecimal amount, String transactionId) {
        this.userId = userId;
        this.storyId = storyId;
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

    public Long getStoryId() {
        return storyId;
    }

    public void setStoryId(Long storyId) {
        this.storyId = storyId;
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
