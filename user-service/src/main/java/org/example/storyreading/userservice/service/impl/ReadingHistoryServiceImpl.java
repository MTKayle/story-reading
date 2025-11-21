package org.example.storyreading.userservice.service.impl;

import org.example.storyreading.userservice.entity.ReadingHistoryEntity;
import org.example.storyreading.userservice.entity.UserEntity;
import org.example.storyreading.userservice.repository.ReadingHistoryRepository;
import org.example.storyreading.userservice.repository.UserRepository;
import org.example.storyreading.userservice.service.ReadingHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ReadingHistoryServiceImpl implements ReadingHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ReadingHistoryServiceImpl.class);
    private final ReadingHistoryRepository readingHistoryRepository;
    private final UserRepository userRepository;

    public ReadingHistoryServiceImpl(ReadingHistoryRepository readingHistoryRepository, UserRepository userRepository) {
        this.readingHistoryRepository = readingHistoryRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void saveOrUpdateHistory(Long userId, Long storyId, Long chapterId) {
        log.info("📖 Saving/updating reading history - userId: {}, storyId: {}, chapterId: {}", userId, storyId, chapterId);
        
        // Get user first
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.error("❌ User not found with id: {}", userId);
            throw new RuntimeException("User not found with id: " + userId);
        }
        UserEntity user = userOpt.get();
        
        Optional<ReadingHistoryEntity> existingOpt = readingHistoryRepository.findByUserAndStoryId(user, storyId);
        
        if (existingOpt.isPresent()) {
            // Update existing history
            ReadingHistoryEntity existing = existingOpt.get();
            existing.setChapterId(chapterId);
            existing.setLastReadAt(Instant.now());
            readingHistoryRepository.save(existing);
            log.info("✅ Updated reading history - userId: {}, storyId: {}, chapterId: {}", userId, storyId, chapterId);
        } else {
            // Create new history
            ReadingHistoryEntity history = new ReadingHistoryEntity(user, storyId, chapterId);
            readingHistoryRepository.save(history);
            log.info("✅ Created new reading history - userId: {}, storyId: {}, chapterId: {}", userId, storyId, chapterId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingHistoryEntity> getUserHistory(Long userId) {
        // Get user first
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.error("❌ User not found with id: {}", userId);
            throw new RuntimeException("User not found with id: " + userId);
        }
        UserEntity user = userOpt.get();
        
        return readingHistoryRepository.findByUserOrderByLastReadAtDesc(user);
    }

    @Override
    @Transactional
    public void deleteHistory(Long userId, Long storyId) {
        // Get user first
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.error("❌ User not found with id: {}", userId);
            throw new RuntimeException("User not found with id: " + userId);
        }
        UserEntity user = userOpt.get();
        
        readingHistoryRepository.deleteByUserAndStoryId(user, storyId);
        log.info("✅ Deleted reading history - userId: {}, storyId: {}", userId, storyId);
    }

    @Override
    @Transactional
    public void deleteAllHistory(Long userId) {
        // Get user first
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.error("❌ User not found with id: {}", userId);
            throw new RuntimeException("User not found with id: " + userId);
        }
        UserEntity user = userOpt.get();
        
        readingHistoryRepository.deleteByUser(user);
        log.info("✅ Deleted all reading history for userId: {}", userId);
    }
}

