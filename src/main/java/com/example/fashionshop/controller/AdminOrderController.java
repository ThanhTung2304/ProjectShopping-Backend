package com.example.fashionshop.controller;

import com.example.fashionshop.dto.ApiResponse;
import com.example.fashionshop.dto.order.OrderDto;
import com.example.fashionshop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDto.Response>> getOrderDetail(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.adminGetOrderDetail(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderDto.Summary>>> getAllOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "orderedAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getAllOrders(keyword, status, pageable)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderDto.Response>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderDto.UpdateStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cap nhat trang thai thanh cong",
                orderService.updateStatus(id, request)));
    }
}
