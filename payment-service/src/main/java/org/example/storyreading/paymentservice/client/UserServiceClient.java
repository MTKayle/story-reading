package org.example.storyreading.paymentservice.client;

import org.example.storyreading.paymentservice.dto.DeductBalanceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${user-service.url:http://localhost:8882}")
    private String userServiceUrl;

    public UserServiceClient() {
        this.restTemplate = new RestTemplate();
    }

    public boolean checkAndDeductBalance(Long userId, BigDecimal amount, String transactionId) {
        String url = userServiceUrl + "/api/users/balance/deduct";
        log.info("Calling user-service to deduct balance: url={}, userId={}, amount={}, transactionId={}", 
                url, userId, amount, transactionId);

        try {
            DeductBalanceRequest request = new DeductBalanceRequest(userId, amount, transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<DeductBalanceRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Balance deducted successfully for userId: {}", userId);
                return true;
            } else {
                log.error("Failed to deduct balance. Status: {}, Response: {}", 
                        response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (HttpClientErrorException e) {
            // 4xx errors - client errors (e.g., insufficient balance, bad request)
            HttpStatusCode statusCode = e.getStatusCode();
            String responseBody = e.getResponseBodyAsString();
            log.error("Client error when deducting balance. Status: {}, Response: {}", statusCode, responseBody);
            
            // Try to extract error message from response
            if (responseBody != null && responseBody.contains("Insufficient balance")) {
                log.error("User {} has insufficient balance. Amount required: {}", userId, amount);
            }
            return false;
        } catch (HttpServerErrorException e) {
            // 5xx errors - server errors
            log.error("Server error when deducting balance. Status: {}, Response: {}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (ResourceAccessException e) {
            // Connection errors (service not available, timeout, etc.)
            log.error("Cannot connect to user-service at {}. Error: {}", url, e.getMessage());
            log.error("Please check if user-service is running on {}", userServiceUrl);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error when deducting balance: {}", e.getMessage(), e);
            return false;
        }
    }
}

