package org.example.storyreading.notificationservice.service;

import org.example.storyreading.notificationservice.dto.comment.CommentEvent;
import org.example.storyreading.notificationservice.dto.deposit.DepositEvent;
import org.example.storyreading.notificationservice.dto.payment.PaymentEvent;
import org.example.storyreading.notificationservice.dto.rating.RatingEvent;
import org.example.storyreading.notificationservice.dto.reaction.ReactionEvent;
import org.example.storyreading.notificationservice.dto.story.NewChapterEvent;
import org.example.storyreading.notificationservice.entity.Notification;

import java.util.List;

public interface NotificationService {
    boolean sendEmail(String to);
    void createCommentNotification(CommentEvent event);
    List<Notification> getNotificationsForUser(Long recipientId);
    void createReactionNotification(ReactionEvent event);
    void createRatingNotification(RatingEvent event);
    void softDeleteByTypeId(Long commentId);
    void markAsRead(Long notificationId, Long recipientId);
    long getUnreadCount(Long recipientId);

    // ✅ 3 methods cho các event mới
    void createDepositNotification(DepositEvent event);
    void createPurchaseStoryNotification(PaymentEvent event);
    void createNewChapterNotification(NewChapterEvent event);
}
