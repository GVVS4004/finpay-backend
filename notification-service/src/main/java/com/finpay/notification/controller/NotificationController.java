package com.finpay.notification.controller;

import com.finpay.notification.dto.NotificationResponse;
import com.finpay.notification.service.NotificationService;
import com.finpay.notification.service.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Real-time and historical notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final SseService sseService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to real-time notifications via SSE")
    public SseEmitter stream(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Token-Expiry") long tokenExpiry) {
        return sseService.subscribe(userId, tokenExpiry);
    }

    @GetMapping
    @Operation(summary = "Get all notifications for the current user")
    public List<NotificationResponse> getNotifications(@RequestHeader("X-User-Id") Long userId) {
        return notificationService.getNotifications(userId);
    }

    @GetMapping("/unread")
    @Operation(summary = "Get unread notifications")
    public List<NotificationResponse> getUnread(@RequestHeader("X-User-Id") Long userId) {
        return notificationService.getUnreadNotifications(userId);
    }

    @GetMapping("/unread/count")
    @Operation(summary = "Get unread notification count")
    public Map<String, Long> getUnreadCount(@RequestHeader("X-User-Id") Long userId) {
        return Map.of("count", notificationService.getUnreadCount(userId));
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark a notification as read")
    public NotificationResponse markAsRead(
            @PathVariable Long notificationId,
            @RequestHeader("X-User-Id") Long userId) {
        return notificationService.markAsRead(notificationId, userId);
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public void markAllAsRead(@RequestHeader("X-User-Id") Long userId) {
        notificationService.markAllAsRead(userId);
    }
}