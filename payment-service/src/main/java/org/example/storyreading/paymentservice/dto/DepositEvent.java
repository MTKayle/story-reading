package org.example.storyreading.paymentservice.dto;

import java.io.Serializable;

public class DepositEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long amount;
    private Long transactionId;

    public DepositEvent() {}

    public DepositEvent(Long userId, Long amount, Long transactionId) {
        this.userId = userId;
        this.amount = amount;
        this.transactionId = transactionId;
    }

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

