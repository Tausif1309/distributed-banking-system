package com.bank.notificationservice.service;

import com.bank.notificationservice.dto.response.MarkAllReadResponse;
import com.bank.notificationservice.dto.response.NotificationResponse;
import com.bank.notificationservice.entity.Notification;
import com.bank.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private final NotificationRepository repository =
            mock(NotificationRepository.class);
    private final NotificationService notificationService =
            new NotificationService(repository);

    @Test
    void listsOnlyTheAuthenticatedUsersNotificationsNewestFirst() {
        Notification notification = notification(41L, 12L, false);
        when(repository.findByUserIdOrderByCreatedAtDesc(12L))
                .thenReturn(List.of(notification));

        List<NotificationResponse> result =
                notificationService.getMyNotifications(12L);

        assertEquals(1, result.size());
        assertEquals(41L, result.getFirst().id());
        verify(repository).findByUserIdOrderByCreatedAtDesc(12L);
    }

    @Test
    void returnsUnreadCountForTheAuthenticatedUser() {
        when(repository.countByUserIdAndReadFalse(12L)).thenReturn(3L);

        assertEquals(3L, notificationService.getMyUnreadCount(12L).unreadCount());
        verify(repository).countByUserIdAndReadFalse(12L);
    }

    @Test
    void marksOnlyAnOwnedNotificationAsRead() {
        Notification notification = notification(41L, 12L, false);
        when(repository.findByIdAndUserId(41L, 12L))
                .thenReturn(Optional.of(notification));
        when(repository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse result =
                notificationService.markAsRead(41L, 12L);

        assertTrue(result.read());
        verify(repository).findByIdAndUserId(41L, 12L);
        verify(repository).save(notification);
    }

    @Test
    void returnsNotFoundForAnotherUsersNotification() {
        when(repository.findByIdAndUserId(41L, 99L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> notificationService.markAsRead(41L, 99L)
        );

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getStatusCode().value());
        verify(repository, never()).save(any(Notification.class));
    }

    @Test
    void marksOnlyTheAuthenticatedUsersUnreadNotificationsAsRead() {
        when(repository.markAllAsReadForUser(12L)).thenReturn(2);

        MarkAllReadResponse result = notificationService.markAllAsRead(12L);

        assertEquals(2, result.updatedCount());
        verify(repository).markAllAsReadForUser(12L);
    }

    private Notification notification(Long id, Long userId, boolean read) {
        return Notification.builder()
                .id(id)
                .userId(userId)
                .transactionId(900L)
                .type("MONEY_RECEIVED")
                .title("Money Received")
                .message("Test notification")
                .read(read)
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();
    }
}
