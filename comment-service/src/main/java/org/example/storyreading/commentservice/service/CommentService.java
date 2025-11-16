package org.example.storyreading.commentservice.service;

import org.example.storyreading.commentservice.dto.comment.CommentRequest;
import org.example.storyreading.commentservice.dto.comment.CommentResponse;
import org.example.storyreading.commentservice.entity.Comment;

import java.util.List;

public interface CommentService {
    CommentResponse createComment(CommentRequest request);
    List<CommentResponse> getCommentsByChapterAndStory(Long chapterId, Long storyId);
    Comment updateComment(Long id, String newContent);
    Comment deleteComment(Long id, Long userId);
    Comment blockComment(Long id);
    void deleteCommentsByStoryId(Long storyId);
    List<CommentResponse> getRootCommentsByStoryId(Long storyId);
}

