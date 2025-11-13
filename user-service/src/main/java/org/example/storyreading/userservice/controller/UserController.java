package org.example.storyreading.userservice.controller;

import org.example.storyreading.userservice.dto.UserDto;
import org.example.storyreading.userservice.entity.UserEntity;
import org.example.storyreading.userservice.repository.UserRepository;
import org.example.storyreading.userservice.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    public UserController(JwtUtils jwtUtils, UserRepository userRepository) {
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
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
        UserDto dto = new UserDto();
        dto.id = user.getId();
        dto.username = user.getUsername();
        dto.email = user.getEmail();
        dto.avatarUrl = user.getAvatarUrl();
        dto.bio = user.getBio();
        dto.role = user.getRole().getName();
        return ResponseEntity.ok(dto);
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
}
