package org.example.storyreading.notificationservice.dto.deposit;

import java.io.Serial;
import java.io.Serializable;

public class DepositEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long amount; // Số tiền nạp (VD: 100000)
    private Long transactionId;

    public DepositEvent() {}

    public DepositEvent(Long userId, Long amount, Long transactionId) {
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

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }
}
