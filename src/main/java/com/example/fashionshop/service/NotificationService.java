package com.example.fashionshop.service;

import com.example.fashionshop.dto.notification.NotificationDto;
import com.example.fashionshop.entity.Notification.NotificationType;
import com.example.fashionshop.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    void create(User user, String title, String message, NotificationType type, Long relatedId);

    Page<NotificationDto.Response> getMyNotifications(String email, Pageable pageable);

    NotificationDto.UnreadCountResponse getUnreadCount(String email);

    NotificationDto.Response markAsRead(String email, Long notificationId);

    void markAllAsRead(String email);
}
