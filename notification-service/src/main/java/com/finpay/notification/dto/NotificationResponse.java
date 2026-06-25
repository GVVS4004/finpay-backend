package com.finpay.notification.dto;

import com.finpay.notification.domain.NotificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {

    private Long id;
    private Long userId;
    private String transactionId;
    private String message;
    private NotificationStatus status;
    private LocalDateTime createdAt;
}