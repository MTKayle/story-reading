package org.example.storyreading.storyservice.controller;

import org.example.storyreading.storyservice.entity.ChapterEntity;
import org.example.storyreading.storyservice.entity.StoryEntity;
import org.example.storyreading.storyservice.repository.ChapterRepository;
import org.example.storyreading.storyservice.repository.StoryRepository;
import org.example.storyreading.storyservice.util.SlugUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/story")
public class StoryContentController {

    private static final Logger log = LoggerFactory.getLogger(StoryContentController.class);

    private final Path PUBLIC_DIR;
    private final Path IMAGES_DIR;

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;

    public StoryContentController(StoryRepository storyRepository,
                                  ChapterRepository chapterRepository,
                                  @Value("${storage.public-dir:public}") String publicDir) {
        this.storyRepository = storyRepository;
        this.chapterRepository = chapterRepository;
        this.PUBLIC_DIR = Path.of(publicDir);
        this.IMAGES_DIR = PUBLIC_DIR.resolve("images");
        log.info("StoryContentController using public dir: {}", this.PUBLIC_DIR.toAbsolutePath());
    }

    @PostMapping("/{storyId}/cover")
    public ResponseEntity<String> uploadCover(@PathVariable Long storyId,
                                              @RequestParam("file") MultipartFile file) throws IOException {
        StoryEntity s = storyRepository.findById(storyId).orElseThrow(() -> new IllegalArgumentException("Story not found"));
        String slug = SlugUtil.slugify(s.getTitle());

        String ext = getExtension(file.getOriginalFilename());
        Path storyDir = IMAGES_DIR.resolve(slug);
        Files.createDirectories(storyDir);
        Path target = storyDir.resolve("cover" + (ext.isEmpty() ? "" : "." + ext));
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        String url = "/public/images/" + slug + "/" + target.getFileName();

        s.setCoverImageId(url);
        storyRepository.save(s);

        log.info("Saved cover to: {} (url={})", target.toAbsolutePath(), url);
        return ResponseEntity.ok(url);
    }

    @PostMapping("/{storyId}/chapters/{chapterNumber}/images")
    public ResponseEntity<List<String>> uploadChapterImages(@PathVariable Long storyId,
                                                            @PathVariable int chapterNumber,
                                                            @RequestParam("files") List<MultipartFile> files) throws IOException {
        StoryEntity s = storyRepository.findById(storyId).orElseThrow(() -> new IllegalArgumentException("Story not found"));
        String slug = SlugUtil.slugify(s.getTitle());

        Path chapterDir = IMAGES_DIR.resolve(slug).resolve(String.valueOf(chapterNumber));
        Files.createDirectories(chapterDir);
        List<String> urls = new ArrayList<>();
        int idx = 1;
        for (MultipartFile f : files) {
            String ext = getExtension(f.getOriginalFilename());
            String filename = String.format("%03d%s", idx++, ext.isEmpty() ? "" : "." + ext);
            Path target = chapterDir.resolve(filename);
            Files.copy(f.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            urls.add("/public/images/" + slug + "/" + chapterNumber + "/" + filename);
            log.info("Saved chapter image to: {}", target.toAbsolutePath());
        }

        // Persist the image URLs into chapter entity (append if exists)
        Optional<ChapterEntity> maybeChapter = chapterRepository.findByStoryAndChapterNumber(s, chapterNumber);
        ChapterEntity chapter;
        if (maybeChapter.isPresent()) {
            chapter = maybeChapter.get();
            String existing = chapter.getImageIds();
            List<String> all;
            if (existing == null || existing.isEmpty()) {
                all = new ArrayList<>(urls);
            } else {
                all = new ArrayList<>(Arrays.asList(existing.split(",")));
                all.addAll(urls);
            }
            chapter.setImageIds(String.join(",", all));
        } else {
            chapter = new ChapterEntity();
            chapter.setStory(s);
            chapter.setChapterNumber(chapterNumber);
            chapter.setTitle("Chapter " + chapterNumber);
            chapter.setImageIds(String.join(",", urls));
        }
        chapterRepository.save(chapter);

        return ResponseEntity.ok(urls);
    }

    private String getExtension(String original) {
        if (!StringUtils.hasText(original) || !original.contains(".")) return "";
        String ext = original.substring(original.lastIndexOf('.') + 1);
        return ext.toLowerCase();
    }
}
