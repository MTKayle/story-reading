package org.example.storyreading.commentservice.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentWithReportCountResponse {
    private Long id;
    private Long storyId;
    private Long chapterId;
    private Long userId;
    private Long parentId;
    private String content;
    private String isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long reportCount; // Số lượng report
}

