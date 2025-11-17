package org.example.storyreading.storyservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "favourite-service", url = "${favourite-service.url:http://localhost:8088}")
public interface FavouriteServiceClient {

    @GetMapping("/api/follows/story/{storyId}/followers")
    List<Long> getStoryFollowers(@PathVariable("storyId") Long storyId);
}

