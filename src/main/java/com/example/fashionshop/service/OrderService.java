package com.example.fashionshop.service;

import com.example.fashionshop.dto.order.OrderDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderDto.Response placeOrder(String email, OrderDto.PlaceOrderRequest request, HttpServletRequest httpRequest);

    Page<OrderDto.Summary> getMyOrders(String email, Pageable pageable);

    OrderDto.Response getOrderDetail(String email, Long orderId);

    void cancelOrder(String email, Long orderId);

    void confirmReceived(String email, Long orderId);

    Page<OrderDto.Summary> getAllOrders(String keyword, String status, Pageable pageable);

    OrderDto.Response updateStatus(Long orderId, OrderDto.UpdateStatusRequest request);

    OrderDto.Response adminGetOrderDetail(Long orderId);
}