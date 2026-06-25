package com.finpay.notification.service;

import com.finpay.notification.domain.Notification;
import com.finpay.notification.domain.NotificationStatus;
import com.finpay.notification.dto.NotificationResponse;
import com.finpay.notification.kafka.PaymentEvent;
import com.finpay.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public void createNotification(PaymentEvent event) {
        String message = buildMessage(event);

        // for transfers notify both sender and receiver
        if (event.getFromUserId() != null) {
            saveAndPublish(event.getFromUserId(), event.getTransactionId(), message);
        }
        if (event.getToUserId() != null && !event.getToUserId().equals(event.getFromUserId())) {
            saveAndPublish(event.getToUserId(), event.getTransactionId(), message);
        }
    }

    private void saveAndPublish(Long userId, String transactionId, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .transactionId(transactionId)
                .message(message)
                .build();

        notificationRepository.save(notification);
        log.info("Notification saved for user: {} transaction: {}", userId, transactionId);

        redisTemplate.convertAndSend("notifications:" + userId, message);
    }

    public List<NotificationResponse> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, NotificationStatus.UNREAD)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD);
    }

    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));

        notification.setStatus(NotificationStatus.READ);
        return mapToResponse(notificationRepository.save(notification));
    }

    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, NotificationStatus.UNREAD);
        unread.forEach(n -> n.setStatus(NotificationStatus.READ));
        notificationRepository.saveAll(unread);
    }
    private String buildMessage(PaymentEvent event) {
        return switch (event.getType()) {
            case "TRANSFER" -> "COMPLETED".equals(event.getStatus())
                    ? String.format("Transfer of ₹%s to %s was successful", event.getAmount(), event.getToAccount())
                    : String.format("Transfer of ₹%s to %s failed", event.getAmount(), event.getToAccount());
            case "DEPOSIT" -> String.format("Deposit of ₹%s was successful", event.getAmount());
            case "WITHDRAWAL" -> "COMPLETED".equals(event.getStatus())
                    ? String.format("Withdrawal of ₹%s was successful", event.getAmount())
                    : String.format("Withdrawal of ₹%s failed", event.getAmount());
            default -> String.format("Transaction %s status: %s", event.getTransactionId(), event.getStatus());
        };
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .transactionId(notification.getTransactionId())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}