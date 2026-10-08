package com.bank.notificationservice.controller;

import com.bank.notificationservice.dto.response.MarkAllReadResponse;
import com.bank.notificationservice.dto.response.NotificationResponse;
import com.bank.notificationservice.dto.response.UnreadCountResponse;
import com.bank.notificationservice.security.SecurityUtils;
import com.bank.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/my")
    public List<NotificationResponse> getMyNotifications(
            Authentication authentication) {
        return notificationService.getMyNotifications(
                SecurityUtils.getCurrentUserId(authentication)
        );
    }

    @GetMapping("/my/unread-count")
    public UnreadCountResponse getMyUnreadCount(
            Authentication authentication) {
        return notificationService.getMyUnreadCount(
                SecurityUtils.getCurrentUserId(authentication)
        );
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        return notificationService.markAsRead(
                id,
                SecurityUtils.getCurrentUserId(authentication)
        );
    }

    @PatchMapping("/my/read-all")
    public MarkAllReadResponse markAllAsRead(
            Authentication authentication) {
        return notificationService.markAllAsRead(
                SecurityUtils.getCurrentUserId(authentication)
        );
    }
}
