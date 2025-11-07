package org.example.storyreading.storyservice.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chapters", uniqueConstraints = @UniqueConstraint(name = "uk_story_chapter_number", columnNames = {"story_id", "chapter_number"}))
public class ChapterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private StoryEntity story;

    @Column(name = "chapter_number", nullable = false)
    private int chapterNumber;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "image_ids", columnDefinition = "TEXT")
    private String imageIds; // comma-separated image ids stored at content-service

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public StoryEntity getStory() { return story; }
    public void setStory(StoryEntity story) { this.story = story; }
    public int getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(int chapterNumber) { this.chapterNumber = chapterNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getImageIds() { return imageIds; }
    public void setImageIds(String imageIds) { this.imageIds = imageIds; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
