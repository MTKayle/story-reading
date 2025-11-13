package org.example.storyreading.notificationservice.repository;

import org.example.storyreading.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientId(Long recipientId);
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    List<Notification> findByRecipientIdAndIsDeletedFalseOrderByCreatedAtDesc(Long recipientId);
    @Modifying
    @Query("UPDATE Notification n SET n.isDeleted = true WHERE n.typeId = :typeId")
    void softDeleteByTypeId(@Param("typeId") Long typeId);
}
