package org.example.storyreading.notificationservice.service.imppl;

import jakarta.transaction.Transactional;
import org.example.storyreading.notificationservice.dto.comment.CommentEvent;
import org.example.storyreading.notificationservice.dto.rating.RatingEvent;
import org.example.storyreading.notificationservice.dto.reaction.ReactionEvent;
import org.example.storyreading.notificationservice.entity.Notification;
import org.example.storyreading.notificationservice.repository.NotificationRepository;
import org.example.storyreading.notificationservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public boolean sendEmail(String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        try{
            message.setTo(to);
            message.setText("Thanh toán thành công truyên");
            message.setSubject("THÔNG BÁO THANH TOÁN HỌC PHÍ THÀNH CÔNG");
            mailSender.send(message);
            return true;
        }
        catch (Exception e){
            throw new MailSendException("Failed to send email to " + to + ": " + e.getMessage());
        }
    }

    @Autowired
    private NotificationRepository repository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void createCommentNotification(CommentEvent event) {
        System.out.println("📢 Processing comment notification event:");
        System.out.println("  - CommentId: " + event.getCommentId());
        System.out.println("  - UserId: " + event.getUserId());
        System.out.println("  - AuthorId: " + event.getAuthorId());
        System.out.println("  - ParentId: " + event.getParentId());
        System.out.println("  - ParentUserId: " + event.getParentUserId());
        System.out.println("  - StoryId: " + event.getStoryId());
        
        // 1. Notification cho tác giả truyện (chỉ khi comment root, không phải reply)
        if (event.getAuthorId() != null && 
            event.getUserId() != null && 
            !event.getUserId().equals(event.getAuthorId()) &&
            event.getParentId() == null) { // Chỉ gửi cho tác giả nếu là root comment
            
            try {
                Notification n1 = Notification.builder()
                        .recipientId(event.getAuthorId())
                        .senderId(event.getUserId())
                        .content("Người dùng " + event.getUserId() + " đã bình luận vào truyện của bạn.\n" + event.getContent())
                        .link("/story/" + event.getStoryId() + "/comments#" + event.getCommentId())
                        .typeId(event.getCommentId())
                        .build();
                
                System.out.println("📢 Saving notification for author (userId: " + event.getAuthorId() + ")");
                System.out.println("📢 Notification: " + n1.toString());
                
                Notification saved = repository.save(n1);
                System.out.println("✅ Notification saved to database with ID: " + saved.getId());
                
                messagingTemplate.convertAndSend("/topic/notifications/" + n1.getRecipientId(), n1);
                System.out.println("✅ Notification sent via WebSocket to user: " + n1.getRecipientId());
            } catch (Exception e) {
                System.err.println("❌ Failed to save/send notification to author: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ Skipped author notification - authorId: " + event.getAuthorId() + ", parentId: " + event.getParentId());
        }

        // 2. Notification cho người bị reply (khi có reply)
        if (event.getParentId() != null && 
            event.getParentUserId() != null && 
            event.getUserId() != null &&
            !event.getParentUserId().equals(event.getUserId())) {
            
            try {
                Notification n2 = Notification.builder()
                        .recipientId(event.getParentUserId())
                        .senderId(event.getUserId())
                        .content("Người dùng " + event.getUserId() + " đã trả lời bình luận của bạn.\n" + event.getContent())
                        .link("/story/" + event.getStoryId() + "/comments#" + event.getCommentId())
                        .typeId(event.getCommentId())
                        .build();
                
                System.out.println("📢 Saving notification for parent user (userId: " + event.getParentUserId() + ")");
                System.out.println("📢 Notification: " + n2.toString());
                
                Notification saved = repository.save(n2);
                System.out.println("✅ Notification saved to database with ID: " + saved.getId());
                
                messagingTemplate.convertAndSend("/topic/notifications/" + n2.getRecipientId(), n2);
                System.out.println("✅ Notification sent via WebSocket to user: " + n2.getRecipientId());
            } catch (Exception e) {
                System.err.println("❌ Failed to save/send notification to parent user: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ Skipped reply notification - parentId: " + event.getParentId() + ", parentUserId: " + event.getParentUserId());
        }
    }
    @Override
    public void createReactionNotification(ReactionEvent event) {
        // Không gửi notification nếu người thực hiện là chủ nhận
        if (!event.getUserId().equals(event.getAuthorId())) {
            Notification n = Notification.builder()
                    .recipientId(event.getAuthorId())
                    .senderId(event.getUserId())
                    .content("Người dùng " + event.getUserId() + " đã " + event.getType() +
                            " bình luận của bạn")
                    .link(event.getCommentId() != null
                            ? "/story/" + event.getStoryId() + "/comments#" + event.getCommentId()
                            : "/story/" + event.getStoryId())
                    .typeId(event.getReactionId())
                    .build();

            repository.save(n);
            messagingTemplate.convertAndSend("/topic/notifications/" + n.getRecipientId(), n);
            System.out.println("📢 Reaction notification sent: " + n.getContent());
        }
    }

    @Override
    public void createRatingNotification(RatingEvent event) {
        // Không gửi notification nếu người thực hiện là tác giả
        if (!event.getUserId().equals(event.getAuthorId())) {
            Notification n = Notification.builder()
                    .recipientId(event.getAuthorId())
                    .senderId(event.getUserId())
                    .content("Người dùng " + event.getUserId() + " đã đánh giá " + event.getStars() + " sao cho truyện của bạn")
                    .link("/story/" + event.getStoryId())
                    .typeId(event.getRatingId())
                    .build();

            repository.save(n);
            messagingTemplate.convertAndSend("/topic/notifications/" + n.getRecipientId(), n);
            System.out.println("📢 Rating notification sent: " + n.getContent());
        }
    }

    @Override
    public List<Notification> getNotificationsForUser(Long recipientId) {
        return repository.findByRecipientIdAndIsDeletedFalseOrderByCreatedAtDesc(recipientId);
    }

    @Transactional
    @Override
    public void softDeleteByTypeId(Long typeId) {
        try {
            repository.softDeleteByTypeId(typeId);
            System.out.println("✅ Soft-deleted notifications for commentId = " + typeId);
        } catch (Exception e) {
            System.err.println("❌ Failed to soft-delete notifications for commentId = " + typeId + ": " + e.getMessage());
        }
    }

    @Transactional
    @Override
    public void markAsRead(Long notificationId) {
        try {
            Notification notification = repository.findById(notificationId)
                    .orElseThrow(() -> new RuntimeException("Notification not found with id: " + notificationId));
            notification.setIsRead(true);
            repository.save(notification);
            System.out.println("✅ Marked notification as read: " + notificationId);
        } catch (Exception e) {
            System.err.println("❌ Failed to mark notification as read: " + notificationId + ": " + e.getMessage());
            throw e;
        }
    }

}
