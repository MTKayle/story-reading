package org.example.storyreading.commentservice.dto.comment;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentRequest {
    private Long storyId;
    private Long chapterId;
    private Long userId;
    private Long parentId;
    private Long storyAuthorId;
    private String content;
}


