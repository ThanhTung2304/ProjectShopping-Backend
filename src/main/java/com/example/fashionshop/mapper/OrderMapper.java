package com.example.fashionshop.mapper;

import com.example.fashionshop.dto.order.OrderDto;
import com.example.fashionshop.entity.Order;
import com.example.fashionshop.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    // OrderItem Entity → ItemResponse DTO
    // Tất cả field trùng tên → tự map
    OrderDto.ItemResponse toItemResponse(OrderItem orderItem);

    // Order Entity → Response DTO đầy đủ
    @Mapping(target = "items",         source = "orderItems")  // orderItems → items
    @Mapping(target = "paymentMethod", ignore = true)          // lấy từ Payment trong Service
    @Mapping(target = "paymentStatus", ignore = true)          // lấy từ Payment trong Service
    OrderDto.Response toResponse(Order order);

    // Order Entity → Summary DTO (dùng trong danh sách)
    @Mapping(target = "totalItems",    ignore = true)  // đếm trong Service
    @Mapping(target = "paymentMethod", ignore = true)
    @Mapping(target = "paymentStatus", ignore = true)
    OrderDto.Summary toSummary(Order order);

    List<OrderDto.Summary> toSummaryList(List<Order> orders);
}