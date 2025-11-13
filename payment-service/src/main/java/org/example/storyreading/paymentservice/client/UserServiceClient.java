package org.example.storyreading.paymentservice.client;

import org.example.storyreading.paymentservice.dto.DeductBalanceRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Component
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${user-service.url:http://localhost:8082}")
    private String userServiceUrl;

    public UserServiceClient() {
        this.restTemplate = new RestTemplate();
    }

    public boolean checkAndDeductBalance(Long userId, BigDecimal amount, String transactionId) {
        try {
            String url = userServiceUrl + "/api/users/balance/deduct";

            DeductBalanceRequest request = new DeductBalanceRequest(userId, amount, transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<DeductBalanceRequest> entity = new HttpEntity<>(request, headers);

            restTemplate.postForObject(url, entity, Void.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

