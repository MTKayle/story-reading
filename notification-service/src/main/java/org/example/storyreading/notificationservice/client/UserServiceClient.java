package org.example.storyreading.notificationservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
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

    /**
     * Lấy danh sách userId đang follow một truyện
     * @param storyId ID của truyện
     * @return danh sách userId hoặc empty list nếu có lỗi
     */
    @SuppressWarnings("unchecked")
    public List<Long> getFollowersByStoryId(Long storyId) {
        if (storyId == null) {
            log.warn("storyId is null, returning empty list");
            return List.of();
        }

        String url = userServiceUrl + "/api/user/follow/story/" + storyId + "/followers";
        log.info("Calling user-service to get followers: url={}, storyId={}", url, storyId);

        try {
            List<?> response = restTemplate.getForObject(url, List.class);
            if (response != null) {
                // Convert to List<Long> - handle both Integer and Long from JSON deserialization
                List<Long> followerIds = new ArrayList<>();
                for (Object item : response) {
                    if (item instanceof Long) {
                        followerIds.add((Long) item);
                    } else if (item instanceof Integer) {
                        followerIds.add(((Integer) item).longValue());
                    } else if (item instanceof Number) {
                        followerIds.add(((Number) item).longValue());
                    } else {
                        log.warn("Unexpected type in followers list: {}", item.getClass().getName());
                    }
                }
                log.info("✅ Retrieved {} followers for storyId {}", followerIds.size(), storyId);
                return followerIds;
            } else {
                log.warn("⚠️ Empty response when getting followers for storyId: {}", storyId);
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

    /**
     * Lấy username từ userId bằng cách gọi user-service
     * @param userId ID của user
     * @return username hoặc null nếu không tìm thấy
     */
    public String getUsername(Long userId) {
        if (userId == null) {
            return null;
        }

        String url = userServiceUrl + "/api/user/" + userId;
        log.debug("Calling user-service to get username: url={}, userId={}", url, userId);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("username")) {
                String username = (String) response.get("username");
                log.debug("Retrieved username for userId {}: {}", userId, username);
                return username;
            } else {
                log.warn("User response does not contain username for userId: {}", userId);
                return null;
            }
        } catch (HttpClientErrorException e) {
            // 4xx errors - user not found, etc.
            log.warn("User not found or error when getting username for userId {}: {}", userId, e.getMessage());
            return null;
        } catch (HttpServerErrorException e) {
            // 5xx errors - server errors
            log.error("Server error when getting username for userId {}: {}", userId, e.getMessage());
            return null;
        } catch (ResourceAccessException e) {
            // Connection errors (service not available, timeout, etc.)
            log.warn("Cannot connect to user-service at {}. Error: {}", url, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error when getting username for userId {}: {}", userId, e.getMessage(), e);
            return null;
        }
    }
}

