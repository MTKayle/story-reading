package org.example.storyreading.paymentservice.repository;

import org.example.storyreading.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String transactionId);
    Optional<Payment> findByVnpayTxnRef(String vnpayTxnRef);
    List<Payment> findByUserId(Long userId);
    List<Payment> findByUserIdAndStatus(Long userId, Payment.PaymentStatus status);

    // Kiểm tra xem user đã mua truyện thành công chưa
    boolean existsByUserIdAndStoryIdAndPaymentTypeAndStatus(
        Long userId,
        Long storyId,
        Payment.PaymentType paymentType,
        Payment.PaymentStatus status
    );
}
