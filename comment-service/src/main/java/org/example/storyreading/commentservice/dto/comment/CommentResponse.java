package org.example.storyreading.commentservice.dto.comment;

import lombok.Builder;
import lombok.Data;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {
    private Long id;
    private Long storyId;
    private Long chapterId;
    private Long userId;
    private Long parentId;
    private String content;
    private Long storyAuthorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

