package org.example.storyreading.userservice.dto;

import java.time.LocalDateTime;

public class UserDto {
    public Long id;
    public String username;
    public String email;
    public String avatarUrl;
    public String bio;
    public String role;
    public String status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public LocalDateTime lockedAt;
    public String lockReason;
}
