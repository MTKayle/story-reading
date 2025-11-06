package org.example.storyreading.userservice.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "reading_history",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "story_id"})
        })
public class ReadingHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mỗi lịch sử đọc thuộc về 1 user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "story_id", nullable = false)
    private Long storyId;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @Column(name = "last_read_at", nullable = false)
    private Instant lastReadAt = Instant.now();

    public ReadingHistoryEntity() {
    }

    public ReadingHistoryEntity(UserEntity user, Long storyId, Long chapterId) {
        this.user = user;
        this.storyId = storyId;
        this.chapterId = chapterId;
        this.lastReadAt = Instant.now();
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

    public Instant getLastReadAt() {
        return lastReadAt;
    }

    public void setLastReadAt(Instant lastReadAt) {
        this.lastReadAt = lastReadAt;
    }
}
