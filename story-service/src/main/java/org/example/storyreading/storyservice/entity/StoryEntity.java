package org.example.storyreading.storyservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stories")
public class StoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 200)
    private String genres;

    @Column(name = "cover_image_id", length = 100)
    private String coverImageId;

    @Column(name = "author", nullable = false, length = 100)
    private String author;

    @Column(name = "is_paid", nullable = false)
    private boolean paid;

    @Column(name = "price", nullable = false)
    private long price;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0L;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getGenres() { return genres; }
    public void setGenres(String genres) { this.genres = genres; }
    public String getCoverImageId() { return coverImageId; }
    public void setCoverImageId(String coverImageId) { this.coverImageId = coverImageId; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }
    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
