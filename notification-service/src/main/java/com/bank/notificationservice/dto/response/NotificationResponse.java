package com.bank.notificationservice.dto.response;

import com.bank.notificationservice.entity.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long transactionId,
        String type,
        String title,
        String message,
        boolean read,
        LocalDateTime createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTransactionId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
