package com.finpay.notification.repository;

import com.finpay.notification.domain.Notification;
import com.finpay.notification.domain.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, NotificationStatus status);

    long countByUserIdAndStatus(Long userId, NotificationStatus status);
}