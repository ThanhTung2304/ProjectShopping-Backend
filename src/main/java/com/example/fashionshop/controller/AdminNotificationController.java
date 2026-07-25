package com.example.fashionshop.controller;

import com.example.fashionshop.dto.ApiResponse;
import com.example.fashionshop.dto.notification.NotificationDto;
import com.example.fashionshop.entity.Notification.NotificationType;
import com.example.fashionshop.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final NotificationService notificationService;

    @PostMapping("/broadcast")
    public ApiResponse<Void> broadcast(@Valid @RequestBody NotificationDto.BroadcastRequest request) {
        notificationService.broadcastToAllCustomers(
                request.getTitle(),
                request.getMessage(),
                NotificationType.PROMOTION,
                null
        );
        return ApiResponse.ok("Da gui thong bao cho tat ca khach hang");
    }
}