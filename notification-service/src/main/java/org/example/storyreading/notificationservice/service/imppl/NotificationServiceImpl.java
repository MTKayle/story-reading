package org.example.storyreading.notificationservice.service.imppl;

import jakarta.transaction.Transactional;
import org.example.storyreading.notificationservice.dto.comment.CommentEvent;
import org.example.storyreading.notificationservice.dto.deposit.DepositEvent;
import org.example.storyreading.notificationservice.dto.payment.PaymentEvent;
import org.example.storyreading.notificationservice.dto.rating.RatingEvent;
import org.example.storyreading.notificationservice.dto.reaction.ReactionEvent;
import org.example.storyreading.notificationservice.dto.story.NewChapterEvent;
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

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;


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

    @Override
    public void createCommentNotification(CommentEvent event) {
        // 1. Notification cho tác giả truyện
        if (!event.getUserId().equals(event.getAuthorId())) {
            Notification n1 = new Notification();
            n1.setRecipientId(event.getAuthorId());
            n1.setSenderId(event.getUserId());
            n1.setContent("Người dùng " + event.getUserId() + " đã bình luận vào truyện của bạn.\n" + event.getContent());
            n1.setLink("/story/" + event.getStoryId() + "/comments#" + event.getCommentId());
            n1.setTypeId(event.getCommentId());
            n1.setIsRead(false);
            n1.setIsDeleted(false);

            System.out.println(n1);
            repository.save(n1);
            messagingTemplate.convertAndSend("/topic/notifications/" + n1.getRecipientId(), n1);

            // ✅ Gửi realtime số lượng thông báo chưa đọc (tăng lên 1)
            long unreadCount = getUnreadCount(n1.getRecipientId());
            messagingTemplate.convertAndSend("/topic/notifications/unread/" + n1.getRecipientId(), unreadCount);
        }

        // 2. Notification cho người bị reply
        if (event.getParentId() != null && !event.getParentId().equals(event.getUserId())) {
            Notification n2 = new Notification();
            n2.setRecipientId(event.getParentId());
            n2.setSenderId(event.getUserId());
            n2.setContent("Người dùng " + event.getUserId() + " đã trả lời bình luận của bạn.\n" + event.getContent());
            n2.setLink("/story/" + event.getStoryId() + "/comments#" + event.getCommentId());
            n2.setTypeId(event.getCommentId());
            n2.setIsRead(false);
            n2.setIsDeleted(false);

            System.out.println(n2);
            repository.save(n2);
            messagingTemplate.convertAndSend("/topic/notifications/" + n2.getRecipientId(), n2);

            // ✅ Gửi realtime số lượng thông báo chưa đọc (tăng lên 1)
            long unreadCount = getUnreadCount(n2.getRecipientId());
            messagingTemplate.convertAndSend("/topic/notifications/unread/" + n2.getRecipientId(), unreadCount);
        }
    }

    @Override
    public void createReactionNotification(ReactionEvent event) {
        // Không gửi notification nếu người thực hiện là chủ nhận
        if (!event.getUserId().equals(event.getAuthorId())) {
            Notification n = new Notification();
            n.setRecipientId(event.getAuthorId());
            n.setSenderId(event.getUserId());
            n.setContent("Người dùng " + event.getUserId() + " đã " + event.getType() + " bình luận của bạn");
            n.setLink(event.getCommentId() != null
                    ? "/story/" + event.getStoryId() + "/comments#" + event.getCommentId()
                    : "/story/" + event.getStoryId());
            n.setTypeId(event.getReactionId());
            n.setIsRead(false);
            n.setIsDeleted(false);

            repository.save(n);
            messagingTemplate.convertAndSend("/topic/notifications/" + n.getRecipientId(), n);

            // ✅ Gửi realtime số lượng thông báo chưa đọc (tăng lên 1)
            long unreadCount = getUnreadCount(n.getRecipientId());
            messagingTemplate.convertAndSend("/topic/notifications/unread/" + n.getRecipientId(), unreadCount);

            System.out.println("📢 Reaction notification sent: " + n.getContent());
        }
    }

    @Override
    public void createRatingNotification(RatingEvent event) {
        // Không gửi notification nếu người thực hiện là tác giả
        if (!event.getUserId().equals(event.getAuthorId())) {
            Notification n = new Notification();
            n.setRecipientId(event.getAuthorId());
            n.setSenderId(event.getUserId());
            n.setContent("Người dùng " + event.getUserId() + " đã đánh giá " + event.getStars() + " sao cho truyện của bạn");
            n.setLink("/story/" + event.getStoryId());
            n.setTypeId(event.getRatingId());
            n.setIsRead(false);
            n.setIsDeleted(false);

            repository.save(n);
            messagingTemplate.convertAndSend("/topic/notifications/" + n.getRecipientId(), n);

            // ✅ Gửi realtime số lượng thông báo chưa đọc (tăng lên 1)
            long unreadCount = getUnreadCount(n.getRecipientId());
            messagingTemplate.convertAndSend("/topic/notifications/unread/" + n.getRecipientId(), unreadCount);

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
    public void markAsRead(Long notificationId, Long recipientId) {
        repository.markAsRead(notificationId);

        // ✅ Gửi realtime cập nhật số lượng thông báo chưa đọc
        long unreadCount = getUnreadCount(recipientId);
        messagingTemplate.convertAndSend("/topic/notifications/unread/" + recipientId, unreadCount);

        System.out.println("✅ Notification " + notificationId + " marked as read. Unread count: " + unreadCount);
    }

    @Override
    public long getUnreadCount(Long recipientId) {
        return repository.countByRecipientIdAndIsReadFalseAndIsDeletedFalse(recipientId);
    }

    // ✅ Thông báo nạp tiền thành công (DepositEvent)
    @Override
    public void createDepositNotification(DepositEvent event) {
        Notification n = new Notification();
        n.setRecipientId(event.getUserId());
        n.setSenderId(null);
        n.setContent("Bạn đã nạp thành công " + event.getAmount() + " vào tài khoản");
        n.setLink("/user/wallet");
        n.setTypeId(event.getTransactionId());
        n.setIsRead(false);
        n.setIsDeleted(false);

        repository.save(n);
        messagingTemplate.convertAndSend("/topic/notifications/" + n.getRecipientId(), n);

        // ✅ Gửi realtime số lượng thông báo chưa đọc (tăng lên 1)
        long unreadCount = getUnreadCount(n.getRecipientId());
        messagingTemplate.convertAndSend("/topic/notifications/unread/" + n.getRecipientId(), unreadCount);

        System.out.println("💰 Deposit notification sent to userId=" + event.getUserId());
    }

    // ✅ Thông báo mua truyện thành công (PaymentEvent)
    @Override
    public void createPurchaseStoryNotification(PaymentEvent event) {
        Notification n = new Notification();
        n.setRecipientId(event.getUserId());
        n.setSenderId(null);
        n.setContent("Bạn đã mua thành công truyện " + event.getStoryTitle());
        n.setLink("/story/" + event.getStoryId());
        n.setTypeId(event.getTransactionId());
        n.setIsRead(false);
        n.setIsDeleted(false);

        repository.save(n);
        messagingTemplate.convertAndSend("/topic/notifications/" + n.getRecipientId(), n);

        // ✅ Gửi realtime số lượng thông báo chưa đọc (tăng lên 1)
        long unreadCount = getUnreadCount(n.getRecipientId());
        messagingTemplate.convertAndSend("/topic/notifications/unread/" + n.getRecipientId(), unreadCount);

        System.out.println("📖 Purchase notification sent to userId=" + event.getUserId());
    }

    // ✅ Thông báo truyện ra chương mới (NewChapterEvent)
    @Override
    public void createNewChapterNotification(NewChapterEvent event) {
        // Gửi thông báo cho tất cả người theo dõi truyện
        if (event.getFollowerIds() != null && !event.getFollowerIds().isEmpty()) {
            for (Long followerId : event.getFollowerIds()) {
                Notification n = new Notification();
                n.setRecipientId(followerId);
                n.setSenderId(event.getAuthorId());
                n.setContent("Truyện " + event.getStoryTitle() + " đã ra chương mới: " + event.getChapterTitle());
                n.setLink("/story/" + event.getStoryId() + "/chapter/" + event.getChapterId());
                n.setTypeId(event.getChapterId());
                n.setIsRead(false);
                n.setIsDeleted(false);

                repository.save(n);
                messagingTemplate.convertAndSend("/topic/notifications/" + n.getRecipientId(), n);

                // ✅ Gửi realtime số lượng thông báo chưa đọc (tăng lên 1)
                long unreadCount = getUnreadCount(n.getRecipientId());
                messagingTemplate.convertAndSend("/topic/notifications/unread/" + n.getRecipientId(), unreadCount);
            }
            System.out.println("📚 New chapter notification sent to " + event.getFollowerIds().size() + " followers");
        }
    }

}
