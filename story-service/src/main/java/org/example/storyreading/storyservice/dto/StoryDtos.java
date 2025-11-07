package org.example.storyreading.storyservice.dto;

import java.util.List;

public class StoryDtos {
    public static class CreateStoryRequest {
        public String title;
        public String description;
        public List<String> genres;
        public String coverImageId;
        public boolean paid;
        public long price;
        public String author;
    }
    public static class StoryResponse {
        public Long id;
        public String title;
        public String description;
        public List<String> genres;
        public String coverImageId;
        public boolean paid;
        public long price;
        public String author;
    }
    public static class CreateChapterRequest {
        public int chapterNumber;
        public String title;
        public List<String> imageIds;
    }
    public static class ChapterResponse {
        public Long id;
        public Long storyId;
        public int chapterNumber;
        public String title;
        public List<String> imageIds;
    }
    public static class PurchaseRequest { }
    public static class PurchaseResponse {
        public Long storyId;
        public Long userId;
        public boolean purchased;
    }
}


