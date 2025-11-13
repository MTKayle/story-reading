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
        // 1. Notification cho tác giả truyệ
        if (!event.getUserId().equals(event.getAuthorId())) {
            Notification n1 = Notification.builder()
                    .recipientId(event.getAuthorId())
                    .senderId(event.getUserId())
                    .content("Người dùng " + event.getUserId() + " đã bình luận vào truyện của bạn.\n" + event.getContent())
                    .link("/story/" + event.getStoryId() + "/comments#" + event.getCommentId())
                    .typeId(event.getCommentId())
                    .build();
            System.out.println(n1.toString());
            repository.save(n1);
            messagingTemplate.convertAndSend("/topic/notifications/" + n1.getRecipientId(), n1);
        }

        // 2. Notification cho người bị reply
        if (event.getParentId() != null && !event.getParentId().equals(event.getUserId())) {
            Notification n2 = Notification.builder()
                    .recipientId(event.getParentId())
                    .senderId(event.getUserId())
                    .content("Người dùng " + event.getUserId() + " đã trả lời bình luận của bạn.\n" + event.getContent())
                    .link("/story/" + event.getStoryId() + "/comments#" + event.getCommentId())
                    .typeId(event.getCommentId())
                    .build();
            System.out.println(n2.toString());
            repository.save(n2);
            messagingTemplate.convertAndSend("/topic/notifications/" + n2.getRecipientId(), n2);
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

}
