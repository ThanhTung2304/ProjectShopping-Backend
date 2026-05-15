package com.example.fashionshop.repository;

import com.example.fashionshop.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface OrderRepositoryCustom {

    /**
     * Admin filter đơn hàng theo nhiều điều kiện:
     * - status: trạng thái đơn hàng
     * - keyword: tìm theo order_code hoặc tên khách
     * - from / to: lọc theo khoảng thời gian đặt hàng
     */
    Page<Order> filterOrders(
            Order.OrderStatus status,
            String keyword,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );
}
