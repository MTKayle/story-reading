package org.example.storyreading.storyservice.dto;

public class GenreDtos {

    public static class GenreRequest {
        public String name;
        public String description;
    }

    public static class GenreResponse {
        public Long id;
        public String name;
        public String slug;
        public String description;
        public long storyCount;
    }
}

