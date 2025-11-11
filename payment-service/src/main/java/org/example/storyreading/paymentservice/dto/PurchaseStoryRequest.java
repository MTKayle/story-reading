package org.example.storyreading.paymentservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class PurchaseStoryRequest {

    @NotNull(message = "Story ID is required")
    @Positive(message = "Story ID must be positive")
    private Long storyId;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    // No-args constructor
    public PurchaseStoryRequest() {
    }

    // All-args constructor
    public PurchaseStoryRequest(Long storyId, BigDecimal price) {
        this.storyId = storyId;
        this.price = price;
    }

    // Getters and Setters
    public Long getStoryId() {
        return storyId;
    }

    public void setStoryId(Long storyId) {
        this.storyId = storyId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
