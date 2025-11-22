package org.example.storyreading.storyservice.service.impl;

import org.example.storyreading.storyservice.client.UserServiceClient;
import org.example.storyreading.storyservice.config.RabbitMQConfig;
import org.example.storyreading.storyservice.dto.NewChapterEvent;
import org.example.storyreading.storyservice.dto.StoryDtos;
import org.example.storyreading.storyservice.entity.ChapterEntity;
import org.example.storyreading.storyservice.entity.StoryEntity;
import org.example.storyreading.storyservice.event.ChapterEventPublisher;
import org.example.storyreading.storyservice.repository.ChapterRepository;
import org.example.storyreading.storyservice.repository.PurchaseRepository;
import org.example.storyreading.storyservice.repository.StoryRepository;
import org.example.storyreading.storyservice.service.IChapterService;
import org.example.storyreading.storyservice.util.SlugUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ChapterService implements IChapterService {

    private final ChapterRepository chapterRepository;
    private final StoryRepository storyRepository;
    private final PurchaseRepository purchaseRepository;
    private final Path publicImagesDir;
    private final ChapterEventPublisher chapterEventPublisher;
    private final UserServiceClient userServiceClient;
    private final RabbitTemplate rabbitTemplate;

    public ChapterService(ChapterRepository chapterRepository,
                          StoryRepository storyRepository,
                          PurchaseRepository purchaseRepository,
                          ChapterEventPublisher chapterEventPublisher,
                          UserServiceClient userServiceClient,
                          RabbitTemplate rabbitTemplate,
                          @Value("${storage.public-dir:public}") String publicDir) {
        this.chapterRepository = chapterRepository;
        this.storyRepository = storyRepository;
        this.purchaseRepository = purchaseRepository;
        this.chapterEventPublisher = chapterEventPublisher;
        this.userServiceClient = userServiceClient;
        this.rabbitTemplate = rabbitTemplate;
        this.publicImagesDir = Paths.get(publicDir).resolve("images");
    }

    @Override
    public StoryDtos.ChapterResponse createChapter(Long storyId, StoryDtos.CreateChapterRequest request) {
        StoryEntity story = storyRepository.findById(storyId).orElseThrow(() -> new IllegalArgumentException("Story not found"));

        // Service-level uniqueness check: ensure there is no existing chapter with the same number for this story
        Optional<ChapterEntity> existing = chapterRepository.findByStoryAndChapterNumber(story, request.chapterNumber);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Chapter number " + request.chapterNumber + " already exists for story id " + storyId);
        }

        ChapterEntity c = new ChapterEntity();
        c.setStory(story);
        c.setChapterNumber(request.chapterNumber);
        c.setTitle(request.title);
        c.setImageIds(request.imageIds == null ? null : String.join(",", request.imageIds));
        c = chapterRepository.save(c);
        
        // Gửi thông báo qua RabbitMQ cho các user đang follow truyện này
        try {
            NewChapterEvent event = new NewChapterEvent(
                    story.getId(),
                    story.getTitle(),
                    c.getId(),
                    c.getChapterNumber(),
                    c.getTitle()
            );
            
            System.out.println("📚 Publishing new chapter event for story: " + story.getTitle() + " (ID: " + story.getId() + ")");
            System.out.println("📚 Chapter: " + c.getTitle() + " (Number: " + c.getChapterNumber() + ")");
            
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NEW_CHAPTER_EXCHANGE,
                    RabbitMQConfig.NEW_CHAPTER_ROUTING_KEY,
                    event
            );
            
            System.out.println("✅ New chapter event published successfully to exchange: " + RabbitMQConfig.NEW_CHAPTER_EXCHANGE);
        } catch (Exception e) {
            System.err.println("❌ Failed to send new chapter notification: " + e.getMessage());
            e.printStackTrace();
            // Không throw exception để không ảnh hưởng đến việc tạo chapter
        }
        
        return toDto(c);
    }

    @Override
    public List<StoryDtos.ChapterResponse> listChapters(Long storyId) {
        StoryEntity story = storyRepository.findById(storyId).orElseThrow(() -> new IllegalArgumentException("Story not found"));
        return chapterRepository.findByStoryOrderByChapterNumberAsc(story)
                .stream().map(this::toDtoListChapter).collect(Collectors.toList());
    }

    @Override
    public StoryDtos.ChapterResponse getChapterForUser(Long chapterId, Long userId) {
        ChapterEntity chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("Chapter not found"));

        StoryEntity story = chapter.getStory();

        // Kiểm tra nếu truyện là premium (price > 0)
        if (story.getPrice() > 0) {
            // Nếu là chapter 1 thì cho phép đọc miễn phí
            if (chapter.getChapterNumber() == 1) {
                return toDto(chapter);
            }

            // Nếu không phải chapter 1, kiểm tra user đã mua chưa
            if (userId == null) {
                throw new IllegalArgumentException("Truyện premium yêu cầu đăng nhập để đọc");
            }

            boolean hasPurchased = purchaseRepository.existsByUserIdAndStory(userId, story);
            if (!hasPurchased) {
                throw new IllegalArgumentException("Bạn cần mua truyện premium này để đọc chapter " + chapter.getChapterNumber());
            }
        }

        return toDto(chapter);
    }

    @Override
    public StoryDtos.ChapterResponse updateChapter(Long storyId, Long chapterId, StoryDtos.CreateChapterRequest request) {
        StoryEntity story = storyRepository.findById(storyId).orElseThrow(() -> new IllegalArgumentException("Story not found"));
        ChapterEntity chapter = chapterRepository.findById(chapterId).orElseThrow(() -> new IllegalArgumentException("Chapter not found"));
        if (!chapter.getStory().getId().equals(storyId)) {
            throw new IllegalArgumentException("Chapter does not belong to story");
        }

        // If chapter number is changed, ensure uniqueness and move folder
        int oldNumber = chapter.getChapterNumber();
        int newNumber = request.chapterNumber;
        if (oldNumber != newNumber) {
            Optional<ChapterEntity> conflict = chapterRepository.findByStoryAndChapterNumber(story, newNumber);
            if (conflict.isPresent()) {
                throw new IllegalArgumentException("Chapter number " + newNumber + " already exists for story id " + storyId);
            }
            // perform filesystem move if folder exists
            String slug = SlugUtil.slugify(story.getTitle());
            Path storyDir = publicImagesDir.resolve(slug);
            Path oldDir = storyDir.resolve(String.valueOf(oldNumber));
            Path newDir = storyDir.resolve(String.valueOf(newNumber));
            try {
                if (Files.exists(oldDir)) {
                    Files.createDirectories(newDir.getParent());
                    Files.move(oldDir, newDir, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to rename chapter folder on disk: " + e.getMessage(), e);
            }
            chapter.setChapterNumber(newNumber);
        }

        // update other fields
        chapter.setTitle(request.title == null ? chapter.getTitle() : request.title);
        chapter.setImageIds(request.imageIds == null ? chapter.getImageIds() : String.join(",", request.imageIds));

        chapter = chapterRepository.save(chapter);
        return toDto(chapter);
    }

    @Override
    public void deleteChapter(Long storyId, Long chapterId) {
        StoryEntity story = storyRepository.findById(storyId).orElseThrow(() -> new IllegalArgumentException("Story not found"));
        ChapterEntity chapter = chapterRepository.findById(chapterId).orElseThrow(() -> new IllegalArgumentException("Chapter not found"));
        if (!chapter.getStory().getId().equals(storyId)) {
            throw new IllegalArgumentException("Chapter does not belong to story");
        }

        // remove files on disk: /{publicDir}/images/{slug}/{chapterNumber}
        String slug = SlugUtil.slugify(story.getTitle());
        Path chapterDir = publicImagesDir.resolve(slug).resolve(String.valueOf(chapter.getChapterNumber()));
        try {
            if (Files.exists(chapterDir)) {
                try (Stream<Path> walk = Files.walk(chapterDir)) {
                    walk.sorted(Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ex) {
                                    throw new RuntimeException("Failed to delete file: " + path + " -> " + ex.getMessage(), ex);
                                }
                            });
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete chapter folder on disk: " + e.getMessage(), e);
        }

        chapterRepository.delete(chapter);
    }

    private StoryDtos.ChapterResponse toDto(ChapterEntity c) {
        StoryDtos.ChapterResponse dto = new StoryDtos.ChapterResponse();
        dto.id = c.getId();
        dto.storyId = c.getStory().getId();
        dto.chapterNumber = c.getChapterNumber();
        dto.title = c.getTitle();
        dto.imageIds = c.getImageIds() == null || c.getImageIds().isEmpty() ? null : Arrays.asList(c.getImageIds().split(","));
        return dto;
    }

    private StoryDtos.ChapterResponse toDtoListChapter(ChapterEntity c) {
        StoryDtos.ChapterResponse dto = new StoryDtos.ChapterResponse();
        dto.id = c.getId();
        dto.storyId = c.getStory().getId();
        dto.chapterNumber = c.getChapterNumber();
        dto.title = c.getTitle();
        dto.imageIds = null;
        return dto;
    }
}
