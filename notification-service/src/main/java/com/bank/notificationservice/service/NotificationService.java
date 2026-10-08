package com.bank.notificationservice.service;

import com.bank.notificationservice.dto.response.MarkAllReadResponse;
import com.bank.notificationservice.dto.response.NotificationResponse;
import com.bank.notificationservice.dto.response.UnreadCountResponse;
import com.bank.notificationservice.entity.Notification;
import com.bank.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<NotificationResponse> getMyNotifications(Long userId) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public UnreadCountResponse getMyUnreadCount(Long userId) {
        return new UnreadCountResponse(
                notificationRepository.countByUserIdAndReadFalse(userId)
        );
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notification not found"
                ));

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }

        return NotificationResponse.from(notification);
    }

    @Transactional
    public MarkAllReadResponse markAllAsRead(Long userId) {
        return new MarkAllReadResponse(
                notificationRepository.markAllAsReadForUser(userId)
        );
    }
}
