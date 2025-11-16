package org.example.storyreading.notificationservice.controller;

import org.example.storyreading.notificationservice.entity.Notification;
import org.example.storyreading.notificationservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal/test")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/user/{recipientId}")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable Long recipientId) {
        List<Notification> notifications = notificationService.getNotificationsForUser(recipientId);
        return ResponseEntity.ok(notifications);
    }

    // ✅ Đánh dấu thông báo đã đọc
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<String> markAsRead(
            @PathVariable Long notificationId,
            @RequestParam Long recipientId) {
        notificationService.markAsRead(notificationId, recipientId);
        return ResponseEntity.ok("Đã đánh dấu đã đọc");
    }

    // ✅ Lấy số lượng thông báo chưa đọc
    @GetMapping("/user/{recipientId}/unread-count")
    public ResponseEntity<Long> getUnreadCount(@PathVariable Long recipientId) {
        long count = notificationService.getUnreadCount(recipientId);
        return ResponseEntity.ok(count);
    }
}
