package org.example.storyreading.paymentservice.dto;

public class VNPayResponse {
    private String paymentUrl;
    private String transactionId;
    private String message;

    public VNPayResponse() {
    }

    public VNPayResponse(String paymentUrl, String transactionId, String message) {
        this.paymentUrl = paymentUrl;
        this.transactionId = transactionId;
        this.message = message;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
