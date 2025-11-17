package org.example.storyreading.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "story-service", url = "${story-service.url:http://localhost:8083}")
public interface StoryServiceClient {

    @GetMapping("/api/story/{storyId}/title")
    String getStoryTitle(@PathVariable("storyId") Long storyId);
}

