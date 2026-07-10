package com.example.fashionshop.dto.notification;

import com.example.fashionshop.entity.Notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class NotificationDto {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String title;
        private String message;
        private NotificationType type;
        private Long relatedId;
        private Boolean isRead;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class UnreadCountResponse {
        private long unreadCount;
    }
}
