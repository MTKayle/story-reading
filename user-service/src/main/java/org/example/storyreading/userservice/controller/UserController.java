package org.example.storyreading.userservice.controller;

import org.example.storyreading.userservice.dto.UpdateUserRequest;
import org.example.storyreading.userservice.dto.UserDto;
import org.example.storyreading.userservice.entity.RoleEntity;
import org.example.storyreading.userservice.entity.UserEntity;
import org.example.storyreading.userservice.entity.UserStatus;
import org.example.storyreading.userservice.repository.RoleRepository;
import org.example.storyreading.userservice.repository.UserRepository;
import org.example.storyreading.userservice.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {
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
        dto.role = user.getRole().getName();
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

    @PutMapping("/admin/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable Long userId, @RequestBody UpdateUserRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.username != null) {
            String username = request.username.trim();
            if (username.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username không được để trống"));
            }
            if (userRepository.existsByUsernameAndIdNot(username, userId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username đã tồn tại"));
            }
            user.setUsername(username);
        }

        if (request.email != null) {
            String email = request.email.trim();
            if (!email.isEmpty()) {
                if (!email.contains("@")) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Email không hợp lệ"));
                }
                if (userRepository.existsByEmailAndIdNot(email, userId)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Email đã tồn tại"));
                }
                user.setEmail(email);
            } else {
                user.setEmail(null);
            }
        }

        if (request.bio != null) {
            user.setBio(request.bio.trim().isEmpty() ? null : request.bio.trim());
        }

        if (request.avatarUrl != null) {
            user.setAvatarUrl(request.avatarUrl.trim().isEmpty() ? null : request.avatarUrl.trim());
        }

        if (request.role != null) {
            RoleEntity role = roleRepository.findByName(request.role.toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException("Role không tồn tại"));
            user.setRole(role);
        }

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok(toDto(user));
    }

}
