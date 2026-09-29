package com.bank.notificationservice.repository;

import com.bank.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(
            Long userId
    );

    boolean existsByTransactionIdAndUserIdAndType(
            Long transactionId,
            Long userId,
            String type
    );
}