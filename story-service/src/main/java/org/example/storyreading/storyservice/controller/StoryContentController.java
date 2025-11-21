package org.example.storyreading.storyservice.controller;

import org.example.storyreading.storyservice.entity.ChapterEntity;
import org.example.storyreading.storyservice.entity.StoryEntity;
import org.example.storyreading.storyservice.repository.ChapterRepository;
import org.example.storyreading.storyservice.repository.StoryRepository;
import org.example.storyreading.storyservice.util.SlugUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/story")
public class StoryContentController {

    private static final Logger log = LoggerFactory.getLogger(StoryContentController.class);

    private final Path IMAGES_DIR;

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;

    public StoryContentController(StoryRepository storyRepository,
                                  ChapterRepository chapterRepository,
                                  @Value("${storage.public-dir:public}") String publicDir) {
        this.storyRepository = storyRepository;
        this.chapterRepository = chapterRepository;
        this.IMAGES_DIR = Path.of(publicDir).resolve("images");
        log.info("StoryContentController using images dir: {}", this.IMAGES_DIR.toAbsolutePath());
    }

    @PostMapping("/{storyId}/cover")
    public ResponseEntity<String> uploadCover(@PathVariable Long storyId,
                                              @RequestParam("file") MultipartFile file) throws IOException {
        StoryEntity s = storyRepository.findById(storyId).orElseThrow(() -> new IllegalArgumentException("Story not found"));
        String slug = getSlugFromStory(s);

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
        String slug = getSlugFromStory(s);

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

    // Delete one or multiple images from a chapter (by filename or by index)
    @DeleteMapping("/{storyId}/chapters/{chapterNumber}/images")
    public ResponseEntity<List<String>> deleteChapterImages(
            @PathVariable Long storyId,
            @PathVariable int chapterNumber,
            @RequestParam(required = false) List<String> filename,
            @RequestParam(required = false) Integer index) {
        StoryEntity s = storyRepository.findById(storyId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found"));
        String slug = getSlugFromStory(s);
        Path chapterDir = IMAGES_DIR.resolve(slug).resolve(String.valueOf(chapterNumber));

        ChapterEntity chapter = chapterRepository.findByStoryAndChapterNumber(s, chapterNumber).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));
        String existing = chapter.getImageIds();
        if (existing == null || existing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No images to delete");
        }
        List<String> images = new ArrayList<>(Arrays.asList(existing.split(",")));

        List<String> removed = new ArrayList<>();
        try {
            if (filename != null && !filename.isEmpty()) {
                // accept filenames or full urls
                for (String fn : filename) {
                    String fnTrim = fn.trim();
                    String targetUrl;
                    if (fnTrim.startsWith("/")) {
                        targetUrl = fnTrim;
                      } else if (fnTrim.contains("/public/images/")) {
                        targetUrl = fnTrim.substring(fnTrim.indexOf("/public/images/"));
                      } else {
                        targetUrl = "/public/images/" + slug + "/" + chapterNumber + "/" + fnTrim;
                      }
                    int idxToRemove = images.indexOf(targetUrl);
                    if (idxToRemove >= 0) {
                        // delete file on disk
                        String fileName = targetUrl.substring(targetUrl.lastIndexOf('/') + 1);
                        Path targetPath = chapterDir.resolve(fileName);
                        Files.deleteIfExists(targetPath);
                        removed.add(targetUrl);
                        images.remove(idxToRemove);
                    }
                }
            } else if (index != null) {
                int i = index - 1; // 1-based to 0-based
                if (i < 0 || i >= images.size()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Index out of range");
                }
                String targetUrl = images.get(i);
                String fileName = targetUrl.substring(targetUrl.lastIndexOf('/') + 1);
                Path targetPath = chapterDir.resolve(fileName);
                Files.deleteIfExists(targetPath);
                removed.add(targetUrl);
                images.remove(i);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide filename(s) or index to delete");
            }

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete image files: " + e.getMessage());
        }

        // Normalize remaining image paths to slug-based structure
        List<String> normalizedImages = normalizeImagePaths(images, slug, chapterNumber);

        chapter.setImageIds(normalizedImages.isEmpty() ? null : String.join(",", normalizedImages));
        chapterRepository.save(chapter);
        List<String> normalizedRemoved = normalizeImagePaths(removed, slug, chapterNumber);
        return ResponseEntity.ok(normalizedRemoved);
    }

    // Replace an existing image (by filename or index) with uploaded file. Keep the filename so order remains.
//    @PutMapping("/{storyId}/chapters/{chapterNumber}/images/replace")
//    public ResponseEntity<String> replaceChapterImage(
//            @PathVariable Long storyId,
//            @PathVariable int chapterNumber,
//            @RequestParam(required = false) String filename,
//            @RequestParam(required = false) Integer index,
//            @RequestParam("file") MultipartFile file) {
//        StoryEntity s = storyRepository.findById(storyId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found"));
//        String slug = SlugUtil.slugify(s.getTitle());
//        Path chapterDir = IMAGES_DIR.resolve(slug).resolve(String.valueOf(chapterNumber));
//
//        ChapterEntity chapter = chapterRepository.findByStoryAndChapterNumber(s, chapterNumber).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));
//        String existing = chapter.getImageIds();
//        if (existing == null || existing.isEmpty()) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No images to replace");
//        }
//        List<String> images = new ArrayList<>(Arrays.asList(existing.split(",")));
//
//        String targetFileName = null;
//        int targetIndex = -1;
//        if (filename != null && !filename.isEmpty()) {
//            // allow filename or full URL
//            String fnTrim = filename.trim();
//            String targetUrl;
//            if (fnTrim.startsWith("/")) {
//                targetUrl = fnTrim;
//            } else if (fnTrim.contains("/public/images/")) {
//                targetUrl = fnTrim.substring(fnTrim.indexOf("/public/images/"));
//            } else {
//                targetUrl = "/public/images/" + slug + "/" + chapterNumber + "/" + fnTrim;
//            }
//            targetIndex = images.indexOf(targetUrl);
//            if (targetIndex < 0) {
//                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filename not found in chapter images");
//            }
//            targetFileName = targetUrl.substring(targetUrl.lastIndexOf('/') + 1);
//        } else if (index != null) {
//            int i = index - 1;
//            if (i < 0 || i >= images.size()) {
//                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Index out of range");
//            }
//            targetIndex = i;
//            String targetUrl = images.get(i);
//            targetFileName = targetUrl.substring(targetUrl.lastIndexOf('/') + 1);
//        } else {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide filename or index to replace");
//        }
//
//        try {
//            Files.createDirectories(chapterDir);
//            Path targetPath = chapterDir.resolve(targetFileName);
//            // Overwrite existing file keeping the same filename so URLs remain consistent and order preserved
//            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
//        } catch (IOException e) {
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write replacement file: " + e.getMessage());
//        }
//
//        // No change needed to images list if filename unchanged; but persist to ensure DB consistency
//        chapter.setImageIds(String.join(",", images));
//        chapterRepository.save(chapter);
//
//        String returnedUrl = "/public/images/" + slug + "/" + chapterNumber + "/" + targetFileName;
//        return ResponseEntity.ok(returnedUrl);
//    }

    private String getExtension(String original) {
        if (!StringUtils.hasText(original) || !original.contains(".")) return "";
        String ext = original.substring(original.lastIndexOf('.') + 1);
        return ext.toLowerCase();
    }

    @PutMapping("/{storyId}/chapters/{chapterNumber}/images/replace")
    public ResponseEntity<List<String>> replaceChapterImages(
            @PathVariable Long storyId,
            @PathVariable int chapterNumber,
            @RequestParam(required = false) List<String> filename,
            @RequestParam(required = false) List<Integer> index,
            @RequestParam("files") List<MultipartFile> files) {

        StoryEntity s = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found"));
        String slug = SlugUtil.slugify(s.getTitle());
        Path chapterDir = IMAGES_DIR.resolve(slug).resolve(String.valueOf(chapterNumber));

        ChapterEntity chapter = chapterRepository.findByStoryAndChapterNumber(s, chapterNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));
        String existing = chapter.getImageIds();
        if (existing == null || existing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No images to replace");
        }
        List<String> images = new ArrayList<>(Arrays.asList(existing.split(",")));

        if ((filename == null || filename.isEmpty()) && (index == null || index.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide filename(s) or index(es) to replace");
        }
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No files uploaded for replacement");
        }

        // Determine mapping between uploaded files and target filenames
        List<String> targetFileNames = new ArrayList<>();
        try {
            if (filename != null && !filename.isEmpty()) {
                if (filename.size() != files.size()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Number of filename parameters must match number of uploaded files");
                }
                for (String fn : filename) {
                    String fnTrim = fn.trim();
                    String targetUrl;
                    if (fnTrim.startsWith("/")) {
                        targetUrl = fnTrim;
                    } else if (fnTrim.contains("/public/images/")) {
                        targetUrl = fnTrim.substring(fnTrim.indexOf("/public/images/"));
                    } else {
                        targetUrl = "/public/images/" + slug + "/" + chapterNumber + "/" + fnTrim;
                    }
                    int targetIndex = images.indexOf(targetUrl);
                    if (targetIndex < 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filename not found in chapter images: " + fnTrim);
                    }
                    String targetFileName = targetUrl.substring(targetUrl.lastIndexOf('/') + 1);
                    targetFileNames.add(targetFileName);
                }
            } else {
                // index provided
                if (index.size() != files.size()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Number of index parameters must match number of uploaded files");
                }
                for (Integer idx : index) {
                    if (idx == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Null index provided");
                    }
                    int i = idx - 1;
                    if (i < 0 || i >= images.size()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Index out of range: " + idx);
                    }
                    String targetUrl = images.get(i);
                    String targetFileName = targetUrl.substring(targetUrl.lastIndexOf('/') + 1);
                    targetFileNames.add(targetFileName);
                }
            }

            // Write each uploaded file to its corresponding target filename (overwrite)
            Files.createDirectories(chapterDir);
            List<String> replacedUrls = new ArrayList<>();
            for (int i = 0; i < files.size(); i++) {
                MultipartFile f = files.get(i);
                String targetFileName = targetFileNames.get(i);
                Path targetPath = chapterDir.resolve(targetFileName);
                Files.copy(f.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                replacedUrls.add("/public/images/" + slug + "/" + chapterNumber + "/" + targetFileName);
                log.info("Replaced chapter image: {}", targetPath.toAbsolutePath());
            }

            // Persist imageIds unchanged (but save to ensure DB sync)
            chapter.setImageIds(String.join(",", images));
            chapterRepository.save(chapter);

            return ResponseEntity.ok(replacedUrls);

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write replacement files: " + e.getMessage());
        }
    }

    private String getSlugFromStory(StoryEntity story) {
        String cover = story.getCoverImageId();
        if (StringUtils.hasText(cover) && cover.contains("/public/images/")) {
            String remainder = cover.substring(cover.indexOf("/public/images/") + "/public/images/".length());
            int slashIndex = remainder.indexOf('/');
            if (slashIndex > 0) {
                return remainder.substring(0, slashIndex);
            }
        }
        return SlugUtil.slugify(story.getTitle());
    }

    private List<String> normalizeImagePaths(List<String> urls, String slug, int chapterNumber) {
        List<String> normalized = new ArrayList<>();
        if (urls == null) return normalized;
        for (String url : urls) {
            if (!StringUtils.hasText(url)) continue;
            String normalizedUrl = url;
            if (!url.contains("/public/images/" + slug + "/" + chapterNumber + "/")) {
                String filename = url.substring(url.lastIndexOf('/') + 1);
                normalizedUrl = "/public/images/" + slug + "/" + chapterNumber + "/" + filename;
            }
            normalized.add(normalizedUrl);
        }
        return normalized;
    }
}
