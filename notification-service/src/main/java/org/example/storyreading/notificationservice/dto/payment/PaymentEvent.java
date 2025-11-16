package org.example.storyreading.notificationservice.dto.payment;

import java.io.Serial;
import java.io.Serializable;

public class PaymentEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long storyId;
    private String storyTitle; // Tên truyện (VD: "Đấu Phá Thương Khung")
    private Long transactionId;

    public PaymentEvent() {}

    public PaymentEvent(Long userId, Long storyId, String storyTitle, Long transactionId) {
        this.userId = userId;
        this.storyId = storyId;
        this.storyTitle = storyTitle;
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

    public String getStoryTitle() {
        return storyTitle;
    }

    public void setStoryTitle(String storyTitle) {
        this.storyTitle = storyTitle;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }
}
