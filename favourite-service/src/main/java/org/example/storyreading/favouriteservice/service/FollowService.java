package org.example.storyreading.favouriteservice.service;

import org.example.storyreading.favouriteservice.dto.FollowDto;
import org.example.storyreading.favouriteservice.entity.Follow;
import org.example.storyreading.favouriteservice.repository.FollowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FollowService {

    @Autowired
    private FollowRepository followRepository;

    @Transactional
    public FollowDto.FollowResponse followStory(Long userId, FollowDto.FollowRequest request) {
        if (followRepository.existsByUserIdAndStoryId(userId, request.getStoryId())) {
            throw new RuntimeException("Already following this story");
        }

        Follow follow = new Follow();
        follow.setUserId(userId);
        follow.setStoryId(request.getStoryId());

        follow = followRepository.save(follow);
        return mapToResponse(follow);
    }

    @Transactional
    public void unfollowStory(Long userId, Long storyId) {
        if (!followRepository.existsByUserIdAndStoryId(userId, storyId)) {
            throw new RuntimeException("Not following this story");
        }
        followRepository.deleteByUserIdAndStoryId(userId, storyId);
    }

    public List<FollowDto.FollowResponse> getUserFollows(Long userId) {
        return followRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public boolean isFollowing(Long userId, Long storyId) {
        return followRepository.existsByUserIdAndStoryId(userId, storyId);
    }

    public FollowDto.FollowStatusResponse getFollowStatus(Long userId, Long storyId) {
        boolean isFollowing = followRepository.existsByUserIdAndStoryId(userId, storyId);
        long followerCount = followRepository.countByStoryId(storyId);
        return new FollowDto.FollowStatusResponse(isFollowing, followerCount);
    }

    public long getStoryFollowerCount(Long storyId) {
        return followRepository.countByStoryId(storyId);
    }

    private FollowDto.FollowResponse mapToResponse(Follow follow) {
        FollowDto.FollowResponse response = new FollowDto.FollowResponse();
        response.setId(follow.getId());
        response.setUserId(follow.getUserId());
        response.setStoryId(follow.getStoryId());
        response.setCreatedAt(follow.getCreatedAt());
        return response;
    }
}

