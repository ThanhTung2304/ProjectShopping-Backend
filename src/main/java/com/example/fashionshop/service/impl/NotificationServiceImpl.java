package com.example.fashionshop.service.impl;

import com.example.fashionshop.dto.notification.NotificationDto;
import com.example.fashionshop.entity.Notification;
import com.example.fashionshop.entity.Notification.NotificationType;
import com.example.fashionshop.entity.User;
import com.example.fashionshop.exception.AppException;
import com.example.fashionshop.exception.ErrorCode;
import com.example.fashionshop.repository.NotificationRepository;
import com.example.fashionshop.repository.ProductRepository;
import com.example.fashionshop.entity.Product;
import com.example.fashionshop.repository.UserRepository;
import com.example.fashionshop.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ProductRepository productRepository;

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

        Notification saved = notificationRepository.save(notification);

        pushRealtime(user, saved); // THÊM MỚI
    }

    @Override
    @Transactional
    public void createForUsers(List<User> users, String title, String message,
                               NotificationType type, Long relatedId) {
        for (User user : users) {
            create(user, title, message, type, relatedId);
        }
    }

    @Override
    public void broadcastToAllCustomers(String title, String message, NotificationType type, Long relatedId) {
        List<User> customers = userRepository.findByRole(User.Role.CUSTOMER);
        createForUsers(customers, title, message, type, relatedId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto.Response> getMyNotifications(String email, Pageable pageable) {

        User user = findUserByEmail(email);

        Page<Notification> page =
                notificationRepository.findByUserId(user.getId(), pageable);

        List<NotificationDto.Response> responses = page.getContent()
                .stream()
                .filter(this::shouldDisplayNotification)
                .map(this::toResponse)
                .toList();

        return new PageImpl<>(
                responses,
                pageable,
                responses.size()
        );
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

    private boolean shouldDisplayNotification(Notification notification) {

        if (notification.getType() != Notification.NotificationType.NEW_PRODUCT) {
            return true;
        }

        if (notification.getRelatedId() == null) {
            return false;
        }

        return productRepository.findById(notification.getRelatedId())
                .map(Product::getIsActive)
                .orElse(false);
    }

    /**
     * Đẩy thông báo real-time tới đúng user qua WebSocket.
     * Client subscribe kênh riêng "/user/queue/notifications" (Spring tự động
     * định tuyến theo Principal/username khi dùng convertAndSendToUser).
     */
    private void pushRealtime(User user, Notification notification) {
        messagingTemplate.convertAndSendToUser(
                user.getEmail(),               // định danh user, phải khớp Principal.getName() lúc kết nối WS
                "/queue/notifications",
                toResponse(notification)
        );
    }
}