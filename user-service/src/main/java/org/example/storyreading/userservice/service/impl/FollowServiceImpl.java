package org.example.storyreading.userservice.service.impl;

import org.example.storyreading.userservice.entity.FollowEntity;
import org.example.storyreading.userservice.entity.UserEntity;
import org.example.storyreading.userservice.repository.FollowRepository;
import org.example.storyreading.userservice.repository.UserRepository;
import org.example.storyreading.userservice.service.FollowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FollowServiceImpl implements FollowService {

    private static final Logger log = LoggerFactory.getLogger(FollowServiceImpl.class);
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public FollowServiceImpl(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public boolean followStory(Long userId, Long storyId) {
        log.info("🔍 Checking if user {} is already following story {}", userId, storyId);
        
        // Check if already following
        if (followRepository.existsByUserIdAndStoryId(userId, storyId)) {
            log.info("ℹ️ User {} is already following story {}", userId, storyId);
            return false; // Already following
        }

        log.info("🔍 Loading user entity for userId: {}", userId);
        // Get user
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.error("❌ User not found with id: {}", userId);
            throw new RuntimeException("User not found with id: " + userId);
        }

        log.info("✅ User found, creating follow entity - userId: {}, storyId: {}", userId, storyId);
        // Create follow entity
        try {
            FollowEntity follow = new FollowEntity(userOpt.get(), storyId);
            followRepository.save(follow);
            log.info("✅ Successfully saved follow entity - userId: {}, storyId: {}", userId, storyId);
            return true;
        } catch (Exception e) {
            log.error("❌ Error saving follow entity - userId: {}, storyId: {}", userId, storyId, e);
            throw new RuntimeException("Failed to save follow: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean unfollowStory(Long userId, Long storyId) {
        Optional<FollowEntity> followOpt = followRepository.findByUserIdAndStoryId(userId, storyId);
        if (followOpt.isEmpty()) {
            return false; // Not following
        }

        followRepository.delete(followOpt.get());
        return true;
    }

    @Override
    public boolean isFollowing(Long userId, Long storyId) {
        return followRepository.existsByUserIdAndStoryId(userId, storyId);
    }

    @Override
    public List<Long> getFollowedStoryIds(Long userId) {
        return followRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(FollowEntity::getStoryId)
                .collect(Collectors.toList());
    }
}

