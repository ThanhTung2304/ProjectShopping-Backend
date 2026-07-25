package com.example.fashionshop.dto.notification;

import com.example.fashionshop.entity.Notification.NotificationType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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

    @Getter
    @Setter
    public static class BroadcastRequest {
        @NotBlank(message = "Tieu de khong duoc de trong")
        private String title;

        @NotBlank(message = "Noi dung khong duoc de trong")
        private String message;
    }
}
