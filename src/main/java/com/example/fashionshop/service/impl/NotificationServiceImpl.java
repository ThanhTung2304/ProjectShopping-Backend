package com.example.fashionshop.service.impl;

import com.example.fashionshop.dto.notification.NotificationDto;
import com.example.fashionshop.entity.Notification;
import com.example.fashionshop.entity.Notification.NotificationType;
import com.example.fashionshop.entity.User;
import com.example.fashionshop.exception.AppException;
import com.example.fashionshop.exception.ErrorCode;
import com.example.fashionshop.repository.NotificationRepository;
import com.example.fashionshop.repository.UserRepository;
import com.example.fashionshop.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void create(User user, String title, String message, NotificationType type, Long relatedId) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .relatedId(relatedId)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto.Response> getMyNotifications(String email, Pageable pageable) {
        User user = findUserByEmail(email);
        return notificationRepository.findByUserId(user.getId(), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationDto.UnreadCountResponse getUnreadCount(String email) {
        User user = findUserByEmail(email);
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(user.getId());
        return NotificationDto.UnreadCountResponse.builder()
                .unreadCount(unreadCount)
                .build();
    }

    @Override
    @Transactional
    public NotificationDto.Response markAsRead(String email, Long notificationId) {
        User user = findUserByEmail(email);
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.setIsRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void markAllAsRead(String email) {
        User user = findUserByEmail(email);
        notificationRepository.markAllAsReadByUserId(user.getId());
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private NotificationDto.Response toResponse(Notification notification) {
        return NotificationDto.Response.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .relatedId(notification.getRelatedId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
