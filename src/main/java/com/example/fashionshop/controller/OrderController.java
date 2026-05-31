package com.example.fashionshop.controller;

import com.example.fashionshop.dto.ApiResponse;
import com.example.fashionshop.dto.order.OrderDto;
import com.example.fashionshop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // POST /api/orders
    // Đặt hàng
    @PostMapping
    public ResponseEntity<ApiResponse<OrderDto.Response>> placeOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OrderDto.PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đặt hàng thành công",
                        orderService.placeOrder(userDetails.getUsername(), request)));
    }

    // GET /api/orders
    // Lịch sử đơn hàng của user
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderDto.Summary>>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10, sort = "orderedAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getMyOrders(userDetails.getUsername(), pageable)));
    }

    // GET /api/orders/{id}
    // Chi tiết đơn hàng
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDto.Response>> getOrderDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getOrderDetail(userDetails.getUsername(), id)));
    }

    // PATCH /api/orders/{id}/cancel
    // Hủy đơn hàng
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        orderService.cancelOrder(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Hủy đơn hàng thành công"));
    }

    // ===== ADMIN =====

    // GET /api/orders/admin
    // Lấy tất cả đơn hàng (admin)
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderDto.Summary>>> getAllOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "orderedAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getAllOrders(keyword, status, pageable)));
    }

    // PATCH /api/orders/admin/{id}/status
    // Cập nhật trạng thái đơn hàng (admin)
    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderDto.Response>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderDto.UpdateStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công",
                orderService.updateStatus(id, request)));
    }
}