package org.example.storyreading.storyservice.service.impl;

import org.example.storyreading.storyservice.dto.StoryDtos;
import org.example.storyreading.storyservice.entity.ChapterEntity;
import org.example.storyreading.storyservice.entity.StoryEntity;
import org.example.storyreading.storyservice.repository.ChapterRepository;
import org.example.storyreading.storyservice.repository.StoryRepository;
import org.example.storyreading.storyservice.service.IStoryService;
import org.example.storyreading.storyservice.util.SlugUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StoryService implements IStoryService {

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final Path imagesDir;

    public StoryService(StoryRepository storyRepository,
                        ChapterRepository chapterRepository,
                        @Value("${storage.public-dir:public}") String publicDir) {
        this.storyRepository = storyRepository;
        this.chapterRepository = chapterRepository;
        this.imagesDir = Path.of(publicDir).resolve("images");
    }

    @Override
    public StoryDtos.StoryResponse createStory(Long userId, StoryDtos.CreateStoryRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        StoryEntity s = new StoryEntity();
        s.setAuthor(String.valueOf(userId)); // Set author from userId header
        s.setTitle(request.title);
        s.setDescription(request.description);
        s.setGenres(request.genres == null ? null : String.join(",", request.genres));
        s.setCoverImageId(request.coverImageId);
        s.setPaid(request.paid);
        s.setPrice(request.price);
        s = storyRepository.save(s);
        return toDto(s);
    }

    @Override
    public StoryDtos.StoryResponse getStory(Long id) {
        StoryEntity s = storyRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Story not found"));
        return toDto(s);
    }

    @Override
    public List<StoryDtos.StoryResponse> listStories() {
        return storyRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StoryDtos.StoryResponse updateStory(Long authorId, Long storyId, StoryDtos.UpdateStoryRequest request) {
        if (authorId == null) throw new IllegalArgumentException("User ID is required");
        StoryEntity s = storyRepository.findById(storyId).orElseThrow(() -> new IllegalArgumentException("Story not found"));

        String oldTitle = s.getTitle();
        String oldSlug = SlugUtil.slugify(oldTitle);
        boolean titleChanged = false;
        String newSlug = oldSlug;

        if (request.title != null && !request.title.equals(oldTitle)) {
            titleChanged = true;
            s.setTitle(request.title);
            newSlug = SlugUtil.slugify(request.title);
        }
        if (request.description != null) s.setDescription(request.description);
        if (request.genres != null) s.setGenres(String.join(",", request.genres));
        if (request.coverImageId != null) s.setCoverImageId(request.coverImageId);
        if (request.paid != null) s.setPaid(request.paid);
        if (request.price != null) s.setPrice(request.price);

        // If title changed, move folder and update image URLs stored in chapters and cover
        if (titleChanged) {
            Path oldDir = imagesDir.resolve(oldSlug);
            Path newDir = imagesDir.resolve(newSlug);
            try {
                if (Files.exists(oldDir)) {
                    // Ensure parent exists
                    Files.createDirectories(newDir.getParent());
                    // Move directory - if newDir exists, move contents
                    if (Files.exists(newDir)) {
                        // copy contents of oldDir into newDir and delete old
                        copyDirectoryContents(oldDir, newDir);
                        deleteDirectoryRecursively(oldDir);
                    } else {
                        Files.move(oldDir, newDir, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to move story image folder: " + e.getMessage(), e);
            }

            // Update coverImageId if it used old slug
            String cover = s.getCoverImageId();
            if (StringUtils.hasText(cover) && cover.contains("/public/images/" + oldSlug)) {
                s.setCoverImageId(cover.replace("/public/images/" + oldSlug, "/public/images/" + newSlug));
            }

            // Update chapters image URLs
            List<ChapterEntity> chapters = chapterRepository.findByStoryOrderByChapterNumberAsc(s);
            for (ChapterEntity ch : chapters) {
                String imgs = ch.getImageIds();
                if (imgs != null && imgs.contains("/public/images/" + oldSlug)) {
                    ch.setImageIds(imgs.replaceAll("/public/images/" + oldSlug, "/public/images/" + newSlug));
                    chapterRepository.save(ch);
                }
            }
        }

        s = storyRepository.save(s);
        return toDto(s);
    }

    @Override
    @Transactional
    public void deleteStory(Long authorId, Long storyId) {
        if (authorId == null) throw new IllegalArgumentException("User ID is required");
        StoryEntity s = storyRepository.findById(storyId).orElseThrow(() -> new IllegalArgumentException("Story not found"));

        // Delete chapters first to satisfy FK constraints
        List<ChapterEntity> chapters = chapterRepository.findByStoryOrderByChapterNumberAsc(s);
        if (!chapters.isEmpty()) {
            chapterRepository.deleteAll(chapters);
        }

        // Delete story row
        storyRepository.delete(s);

        // Delete filesystem folder if exists
        String slug = SlugUtil.slugify(s.getTitle());
        Path storyDir = imagesDir.resolve(slug);
        try {
            if (Files.exists(storyDir)) {
                deleteDirectoryRecursively(storyDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete story image folder: " + e.getMessage(), e);
        }
    }

    @Override
    public List<StoryDtos.StoryResponse> searchByTitle(String title) {
        if (title == null || title.isBlank()) return Collections.emptyList();
        List<StoryEntity> list = storyRepository.findByTitleContainingIgnoreCase(title);
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<StoryDtos.StoryResponse> getStoriesByGenre(String genre, int page, int size) {
        if (genre == null || genre.isBlank()) return Collections.emptyList();
        var pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return storyRepository.findByGenresContainingIgnoreCase(genre, pageable)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private StoryDtos.StoryResponse toDto(StoryEntity s) {
        StoryDtos.StoryResponse dto = new StoryDtos.StoryResponse();
        dto.id = s.getId();
        dto.title = s.getTitle();
        dto.description = s.getDescription();
        dto.genres = s.getGenres() == null || s.getGenres().isEmpty() ? null : Arrays.asList(s.getGenres().split(","));
        dto.coverImageId = s.getCoverImageId();
        dto.paid = s.isPaid();
        dto.price = s.getPrice();
        dto.author = s.getAuthor();
        return dto;
    }

    private void deleteDirectoryRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException e) { /* ignore */ }
            });
        }
    }

    private void copyDirectoryContents(Path source, Path target) throws IOException {
        if (!Files.exists(source)) return;
        Files.createDirectories(target);
        try (var stream = Files.walk(source)) {
            for (Path p : (Iterable<Path>) stream::iterator) {
                if (Files.isDirectory(p)) continue;
                Path relative = source.relativize(p);
                Path dest = target.resolve(relative);
                Files.createDirectories(dest.getParent());
                Files.copy(p, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
