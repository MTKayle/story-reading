package org.example.storyreading.userservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/public/avatars")
public class AvatarFileController {

    private static final Logger log = LoggerFactory.getLogger(AvatarFileController.class);
    private final Path AVATARS_DIR;

    public AvatarFileController(@Value("${storage.public-dir:public}") String publicDir) {
        // Resolve public directory - always resolve from user-service directory
        Path publicPath;
        Path relativePath = Paths.get(publicDir);
        if (relativePath.isAbsolute()) {
            publicPath = relativePath;
        } else {
            // Get the directory where the JAR/classes are running from
            Path currentDir = Paths.get("").toAbsolutePath();
            log.info("AvatarFileController - Current working directory: {}", currentDir);
            
            // If running from user-service directory, go up one level to services, then to public
            if (currentDir.endsWith("user-service")) {
                // Go up to services directory, then resolve publicDir (which is ../public)
                publicPath = currentDir.getParent().resolve("public").normalize();
            } else if (currentDir.endsWith("services")) {
                // Already in services directory
                publicPath = currentDir.resolve("public").normalize();
            } else {
                // Try to resolve from current directory
                publicPath = currentDir.resolve(publicDir).normalize();
            }
        }
        
        this.AVATARS_DIR = publicPath.resolve("avatars");
        log.info("AvatarFileController using avatars dir: {}", this.AVATARS_DIR.toAbsolutePath());
        log.info("Avatars directory exists: {}", Files.exists(this.AVATARS_DIR));
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        try {
            // Security: prevent path traversal
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                log.warn("Invalid filename requested: {}", filename);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            Path filePath = AVATARS_DIR.resolve(filename);
            
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                log.warn("Avatar file not found: {}", filePath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Check if file is within avatars directory (prevent path traversal)
            if (!filePath.normalize().startsWith(AVATARS_DIR.normalize())) {
                log.warn("Path traversal attempt detected: {}", filename);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            Resource resource = new FileSystemResource(filePath);
            
            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                // Fallback based on file extension
                if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (filename.toLowerCase().endsWith(".png")) {
                    contentType = "image/png";
                } else if (filename.toLowerCase().endsWith(".gif")) {
                    contentType = "image/gif";
                } else if (filename.toLowerCase().endsWith(".webp")) {
                    contentType = "image/webp";
                } else {
                    contentType = "application/octet-stream";
                }
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                    .body(resource);
                    
        } catch (IOException e) {
            log.error("Error serving avatar file {}: {}", filename, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

