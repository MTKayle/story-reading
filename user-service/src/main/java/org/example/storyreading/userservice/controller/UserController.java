package org.example.storyreading.userservice.controller;

import org.example.storyreading.userservice.dto.UpdateUserRequest;
import org.example.storyreading.userservice.dto.UserDto;
import org.example.storyreading.userservice.entity.RoleEntity;
import org.example.storyreading.userservice.entity.UserEntity;
import org.example.storyreading.userservice.entity.UserStatus;
import org.example.storyreading.userservice.repository.RoleRepository;
import org.example.storyreading.userservice.repository.UserRepository;
import org.example.storyreading.userservice.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    public UserController(JwtUtils jwtUtils, UserRepository userRepository, RoleRepository roleRepository) {
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    private UserDto toDto(UserEntity user) {
        UserDto dto = new UserDto();
        dto.id = user.getId();
        dto.username = user.getUsername();
        dto.email = user.getEmail();
        dto.avatarUrl = user.getAvatarUrl();
        dto.bio = user.getBio();
        // Ensure role is loaded before accessing
        if (user.getRole() != null) {
            dto.role = user.getRole().getName();
        } else {
            dto.role = "USER"; // Default role
        }
        dto.status = user.getStatus() != null ? user.getStatus().name() : UserStatus.ACTIVE.name();
        dto.createdAt = user.getCreatedAt();
        dto.updatedAt = user.getUpdatedAt();
        dto.lockedAt = user.getLockedAt();
        dto.lockReason = user.getLockReason();
        return dto;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized! Missing or invalid Authorization header"));
        }
        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Access token is invalid or expired"));
        }
        Long userId = jwtUtils.extractUserId(token);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Token has no userId claim"));
        }
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        UserEntity user = userOpt.get();
        return ResponseEntity.ok(toDto(user));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        UserEntity user = userOpt.get();
        return ResponseEntity.ok(toDto(user));
    }

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized! Missing or invalid Authorization header"));
        }
        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Access token is invalid or expired"));
        }
        Long userId = jwtUtils.extractUserId(token);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Token has no userId claim"));
        }
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "User not found"));

        UserEntity user = userOpt.get();
        return ResponseEntity.ok(Map.of(
            "userId", user.getId(),
            "username", user.getUsername(),
            "balance", user.getBalance()
        ));
    }

    // Get all users (for admin)
    @GetMapping("/admin/all")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserEntity> allUsers = userRepository.findAll();
        List<UserDto> userDtos = allUsers.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateUserRequest request) {
        log.info("📝 Update profile request received");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized! Missing or invalid Authorization header"));
        }
        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Access token is invalid or expired"));
        }
        Long userId = jwtUtils.extractUserId(token);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Token has no userId claim"));
        }
        
        // Don't allow users to change their role via /me endpoint
        if (request.role != null) {
            log.warn("⚠️ User {} attempted to change role via /me endpoint", userId);
            request.role = null;
        }
        
        try {
            return updateUserInternal(userId, request);
        } catch (Exception e) {
            log.error("❌ Error updating profile for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Internal server error"));
        }
    }

    @PutMapping("/admin/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable Long userId, @RequestBody UpdateUserRequest request) {
        return updateUserInternal(userId, request);
    }

    @Transactional
    private ResponseEntity<?> updateUserInternal(Long userId, UpdateUserRequest request) {
        log.info("🔍 Updating user profile - userId: {}", userId);
        
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.error("❌ User not found - userId: {}", userId);
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        
        UserEntity user = userOpt.get();

        if (request.username != null) {
            String username = request.username.trim();
            if (username.isEmpty()) {
                log.warn("⚠️ Empty username provided for user {}", userId);
                return ResponseEntity.badRequest().body(Map.of("error", "Username không được để trống"));
            }
            if (userRepository.existsByUsernameAndIdNot(username, userId)) {
                log.warn("⚠️ Username already exists - username: {}, userId: {}", username, userId);
                return ResponseEntity.badRequest().body(Map.of("error", "Username đã tồn tại"));
            }
            log.info("✅ Updating username for user {}: {} -> {}", userId, user.getUsername(), username);
            user.setUsername(username);
        }

        if (request.email != null) {
            String email = request.email.trim();
            if (email.isEmpty()) {
                log.info("✅ Clearing email for user {}", userId);
                user.setEmail(null);
            } else {
                // Basic email validation
                if (!email.contains("@") || !email.contains(".") || email.length() < 5) {
                    log.warn("⚠️ Invalid email format - email: {}", email);
                    return ResponseEntity.badRequest().body(Map.of("error", "Email không hợp lệ"));
                }
                // Check if email already exists for another user
                if (userRepository.existsByEmailAndIdNot(email, userId)) {
                    log.warn("⚠️ Email already exists - email: {}, userId: {}", email, userId);
                    return ResponseEntity.badRequest().body(Map.of("error", "Email đã tồn tại"));
                }
                log.info("✅ Updating email for user {}: {} -> {}", userId, user.getEmail(), email);
                user.setEmail(email);
            }
        }

        if (request.bio != null) {
            String bio = request.bio.trim().isEmpty() ? null : request.bio.trim();
            log.info("✅ Updating bio for user {}", userId);
            user.setBio(bio);
        }

        if (request.avatarUrl != null) {
            String avatarUrl = request.avatarUrl.trim().isEmpty() ? null : request.avatarUrl.trim();
            log.info("✅ Updating avatarUrl for user {}", userId);
            user.setAvatarUrl(avatarUrl);
        }

        if (request.role != null) {
            Optional<RoleEntity> roleOpt = roleRepository.findByName(request.role.toUpperCase());
            if (roleOpt.isEmpty()) {
                log.error("❌ Role not found - role: {}", request.role);
                return ResponseEntity.badRequest().body(Map.of("error", "Role không tồn tại"));
            }
            log.info("✅ Updating role for user {}: {} -> {}", userId, user.getRole().getName(), request.role);
            user.setRole(roleOpt.get());
        }

        user.setUpdatedAt(LocalDateTime.now());
        
        try {
            userRepository.save(user);
            log.info("✅ Successfully updated profile for user {}", userId);
            return ResponseEntity.ok(toDto(user));
        } catch (Exception e) {
            log.error("❌ Error saving user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to save user: " + e.getMessage()));
        }
    }

}
