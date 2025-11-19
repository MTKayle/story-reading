package org.example.storyreading.notificationservice.controller;

import org.example.storyreading.notificationservice.entity.Notification;
import org.example.storyreading.notificationservice.repository.NotificationRepository;
import org.example.storyreading.notificationservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping("/user/{recipientId}")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable Long recipientId) {
        List<Notification> notifications = notificationService.getNotificationsForUser(recipientId);
        return ResponseEntity.ok(notifications);
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long notificationId) {
        try {
            notificationService.markAsRead(notificationId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification marked as read");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // Test endpoint để kiểm tra database connection và lưu notification
    @PostMapping("/test")
    public ResponseEntity<?> testNotification(@RequestParam Long recipientId, 
                                                @RequestParam Long senderId,
                                                @RequestParam String content) {
        try {
            Notification testNotification = Notification.builder()
                    .recipientId(recipientId)
                    .senderId(senderId)
                    .content(content)
                    .link("/test")
                    .typeId(999L)
                    .isDeleted(false)
                    .build();
            
            System.out.println("🧪 Test: Saving notification to database...");
            System.out.println("🧪 Test Notification: " + testNotification.toString());
            
            Notification savedNotification = notificationRepository.save(testNotification);
            System.out.println("✅ Test: Notification saved with ID: " + savedNotification.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test notification saved successfully");
            response.put("notificationId", savedNotification.getId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Test: Failed to save notification: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("errorClass", e.getClass().getName());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
