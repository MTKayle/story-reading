package org.example.storyreading.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class PurchaseStoryRequest {

    @NotNull(message = "Story ID is required")
    @Positive(message = "Story ID must be positive")
    @JsonProperty("storyId")
    private Long storyId;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    @JsonProperty("price")
    private BigDecimal price;

    // Custom constructor để handle conversion từ number/string sang BigDecimal
    @JsonCreator
    public PurchaseStoryRequest(
            @JsonProperty("storyId") Long storyId,
            @JsonProperty("price") Object price) {
        this.storyId = storyId;
        
        // Convert price từ number hoặc string sang BigDecimal
        if (price == null) {
            this.price = null;
        } else if (price instanceof BigDecimal) {
            this.price = (BigDecimal) price;
        } else if (price instanceof Number) {
            this.price = BigDecimal.valueOf(((Number) price).doubleValue());
        } else if (price instanceof String) {
            try {
                this.price = new BigDecimal((String) price);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Price must be a valid number: " + price);
            }
        } else {
            throw new IllegalArgumentException("Price must be a number, got: " + price.getClass().getSimpleName());
        }
    }

    // No-args constructor (for Jackson deserialization fallback)
    public PurchaseStoryRequest() {
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
