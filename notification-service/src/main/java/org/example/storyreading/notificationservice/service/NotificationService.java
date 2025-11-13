package org.example.storyreading.notificationservice.service;

import org.example.storyreading.notificationservice.dto.comment.CommentEvent;
import org.example.storyreading.notificationservice.dto.rating.RatingEvent;
import org.example.storyreading.notificationservice.dto.reaction.ReactionEvent;
import org.example.storyreading.notificationservice.entity.Notification;

import java.util.List;

public interface NotificationService {
    boolean sendEmail(String to);
    void createCommentNotification(CommentEvent event);
    List<Notification> getNotificationsForUser(Long recipientId);
    void createReactionNotification(ReactionEvent event);
    void createRatingNotification(RatingEvent event);
    void softDeleteByTypeId(Long commentId);
}
