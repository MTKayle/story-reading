package org.example.storyreading.storyservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${user-service.url:http://localhost:8882}")
    private String userServiceUrl;

    public UserServiceClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Lấy danh sách userId đang follow một truyện từ user-service
     * @param storyId ID của truyện
     * @return danh sách userId hoặc empty list nếu có lỗi
     */
    @SuppressWarnings("unchecked")
    public List<Long> getStoryFollowers(Long storyId) {
        if (storyId == null) {
            log.warn("storyId is null, returning empty list");
            return List.of();
        }

        String url = userServiceUrl + "/api/user/follow/story/" + storyId + "/followers";
        log.debug("Calling user-service to get followers: url={}, storyId={}", url, storyId);

        try {
            List<Long> response = restTemplate.getForObject(url, List.class);
            if (response != null) {
                log.debug("Retrieved {} followers for storyId {}", response.size(), storyId);
                return response;
            } else {
                log.warn("Empty response when getting followers for storyId: {}", storyId);
                return List.of();
            }
        } catch (HttpClientErrorException e) {
            log.warn("Error when getting followers for storyId {}: {}", storyId, e.getMessage());
            return List.of();
        } catch (HttpServerErrorException e) {
            log.error("Server error when getting followers for storyId {}: {}", storyId, e.getMessage());
            return List.of();
        } catch (ResourceAccessException e) {
            log.warn("Cannot connect to user-service at {}. Error: {}", url, e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("Unexpected error when getting followers for storyId {}: {}", storyId, e.getMessage(), e);
            return List.of();
        }
    }
}

