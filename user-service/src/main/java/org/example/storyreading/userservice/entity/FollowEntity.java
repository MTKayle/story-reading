package org.example.storyreading.userservice.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "follows",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "story_id"})
        })
public class FollowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mỗi lượt theo dõi thuộc về 1 user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // ID truyện từ Story Service (chỉ lưu ID, không quan hệ trực tiếp)
    @Column(name = "story_id", nullable = false)
    private Long storyId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public FollowEntity() {
    }

    public FollowEntity(UserEntity user, Long storyId) {
        this.user = user;
        this.storyId = storyId;
        this.createdAt = Instant.now();
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public Long getStoryId() {
        return storyId;
    }

    public void setStoryId(Long storyId) {
        this.storyId = storyId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
