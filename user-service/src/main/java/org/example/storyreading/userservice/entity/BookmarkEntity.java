package org.example.storyreading.userservice.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "bookmarks",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "story_id", "chapter_id"})
        })
public class BookmarkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mỗi bookmark thuộc về 1 user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "story_id", nullable = false)
    private Long storyId;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public BookmarkEntity() {
    }

    public BookmarkEntity(UserEntity user, Long storyId, Long chapterId) {
        this.user = user;
        this.storyId = storyId;
        this.chapterId = chapterId;
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

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
