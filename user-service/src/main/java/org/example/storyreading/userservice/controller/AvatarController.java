package org.example.storyreading.userservice.controller;

import org.example.storyreading.userservice.entity.UserEntity;
import org.example.storyreading.userservice.repository.UserRepository;
import org.example.storyreading.userservice.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/user/avatar")
public class AvatarController {

    private static final Logger log = LoggerFactory.getLogger(AvatarController.class);
    private final Path AVATARS_DIR;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public AvatarController(
            UserRepository userRepository,
            JwtUtils jwtUtils,
            @Value("${storage.public-dir:public}") String publicDir) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        
        // Resolve public directory - always resolve to services/public
        Path publicPath;
        Path relativePath = Path.of(publicDir);
        if (relativePath.isAbsolute()) {
            publicPath = relativePath;
        } else {
            // Get the directory where the JAR/classes are running from
            Path currentDir = Path.of("").toAbsolutePath();
            log.info("AvatarController - Current working directory: {}", currentDir);
            
            // Always resolve to services/public regardless of where we're running from
            if (currentDir.endsWith(Path.of("user-service"))) {
                // Go up to services directory, then to public
                publicPath = currentDir.getParent().resolve("public").normalize();
            } else if (currentDir.endsWith(Path.of("services"))) {
                // Already in services directory
                publicPath = currentDir.resolve("public").normalize();
            } else {
                // Try to find services directory by going up
                Path tempDir = currentDir;
                while (tempDir != null && !tempDir.endsWith(Path.of("services"))) {
                    tempDir = tempDir.getParent();
                    if (tempDir == null) break;
                }
                if (tempDir != null && tempDir.endsWith(Path.of("services"))) {
                    publicPath = tempDir.resolve("public").normalize();
                } else {
                    // Fallback: resolve from current directory
                    publicPath = currentDir.resolve(publicDir).normalize();
                    log.warn("Could not find services directory, using fallback path: {}", publicPath);
                }
            }
        }
        
        this.AVATARS_DIR = publicPath.resolve("avatars");
        try {
            Files.createDirectories(AVATARS_DIR);
            log.info("AvatarController using avatars dir: {}", this.AVATARS_DIR.toAbsolutePath());
            log.info("Avatars directory exists: {}", Files.exists(this.AVATARS_DIR));
            if (Files.exists(this.AVATARS_DIR)) {
                log.info("Avatars directory is writable: {}", Files.isWritable(this.AVATARS_DIR));
            }
        } catch (IOException e) {
            log.error("Failed to create avatars directory", e);
        }
    }

    private Long extractUserIdFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            return null;
        }
        return jwtUtils.extractUserId(token);
    }

    private String getExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "";
        }
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot < 0 || lastDot >= originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(lastDot + 1).toLowerCase();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("📤 Upload avatar request received - file size: {} bytes", file.getSize());

        Long userId = extractUserIdFromHeader(authHeader);
        if (userId == null) {
            log.warn("❌ Unauthorized - userId is null");
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        // Validate file
        if (file == null || file.isEmpty()) {
            log.warn("⚠️ Empty or null file provided");
            return ResponseEntity.badRequest().body(Map.of("error", "File không được để trống"));
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            log.warn("⚠️ Invalid file type - contentType: {}", contentType);
            return ResponseEntity.badRequest().body(Map.of("error", "Chỉ chấp nhận file ảnh"));
        }

        // Validate file size (max 5MB)
        long fileSize = file.getSize();
        if (fileSize <= 0) {
            log.warn("⚠️ Invalid file size: {}", fileSize);
            return ResponseEntity.badRequest().body(Map.of("error", "File không hợp lệ"));
        }
        if (fileSize > 5 * 1024 * 1024) {
            log.warn("⚠️ File too large - size: {} bytes", fileSize);
            return ResponseEntity.badRequest().body(Map.of("error", "File không được vượt quá 5MB"));
        }

        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.error("❌ User not found - userId: {}", userId);
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        UserEntity user = userOpt.get();

        try {
            // Ensure directory exists
            if (!Files.exists(AVATARS_DIR)) {
                Files.createDirectories(AVATARS_DIR);
                log.info("Created avatars directory: {}", AVATARS_DIR.toAbsolutePath());
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String ext = getExtension(originalFilename);
            // Default to jpg if no extension found
            if (ext.isEmpty()) {
                // Try to determine from content type (contentType already defined above)
                if (contentType != null) {
                    if (contentType.contains("png")) ext = "png";
                    else if (contentType.contains("gif")) ext = "gif";
                    else if (contentType.contains("webp")) ext = "webp";
                    else ext = "jpg";
                } else {
                    ext = "jpg";
                }
            }
            String filename = userId + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
            Path target = AVATARS_DIR.resolve(filename);

            // Delete old avatar file if exists
            String oldAvatarUrl = user.getAvatarUrl();
            if (oldAvatarUrl != null && oldAvatarUrl.startsWith("/public/avatars/")) {
                try {
                    String oldFilename = oldAvatarUrl.substring(oldAvatarUrl.lastIndexOf('/') + 1);
                    Path oldFile = AVATARS_DIR.resolve(oldFilename);
                    if (Files.exists(oldFile)) {
                        Files.delete(oldFile);
                        log.info("✅ Deleted old avatar file: {}", oldFilename);
                    }
                } catch (IOException e) {
                    log.warn("⚠️ Failed to delete old avatar file: {}", e.getMessage());
                }
            }

            // Save file
            log.info("📁 Saving avatar file to: {}", target.toAbsolutePath());
            log.info("📁 AVATARS_DIR: {}", AVATARS_DIR.toAbsolutePath());
            log.info("📁 Target file path: {}", target.toAbsolutePath());
            log.info("📁 Directory exists before save: {}", Files.exists(AVATARS_DIR));
            
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            
            // Verify file was saved
            if (Files.exists(target)) {
                long savedFileSize = Files.size(target);
                log.info("✅ File saved successfully! Size: {} bytes", savedFileSize);
            } else {
                log.error("❌ File was not saved! Target path: {}", target.toAbsolutePath());
            }
            
            String url = "/public/avatars/" + filename;

            // Update user avatar
            user.setAvatarUrl(url);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("✅ Successfully uploaded avatar for user {}: {}", userId, url);
            log.info("✅ Avatar URL saved to database: {}", url);
            return ResponseEntity.ok(Map.of("avatarUrl", url, "message", "Upload avatar thành công"));
        } catch (IOException e) {
            log.error("❌ Error saving avatar file for user {}: {}", userId, e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi khi lưu file: " + e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Unexpected error uploading avatar for user {}: {}", userId, e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi không xác định: " + e.getMessage()));
        }
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<?> deleteAvatar(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("🗑️ Delete avatar request received");

        Long userId = extractUserIdFromHeader(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        UserEntity user = userOpt.get();
        String oldAvatarUrl = user.getAvatarUrl();

        // Delete old avatar file if exists
        if (oldAvatarUrl != null && oldAvatarUrl.startsWith("/public/avatars/")) {
            try {
                String filename = oldAvatarUrl.substring(oldAvatarUrl.lastIndexOf('/') + 1);
                Path oldFile = AVATARS_DIR.resolve(filename);
                if (Files.exists(oldFile)) {
                    Files.delete(oldFile);
                    log.info("✅ Deleted old avatar file: {}", filename);
                }
            } catch (IOException e) {
                log.warn("⚠️ Failed to delete old avatar file: {}", e.getMessage());
            }
        }

        // Clear avatar URL
        user.setAvatarUrl(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("✅ Successfully deleted avatar for user {}", userId);
        return ResponseEntity.ok(Map.of("message", "Đã xóa avatar"));
    }
}

