package com.example.fashionshop.repository;

import com.example.fashionshop.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, OrderRepositoryCustom {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Optional<Order> findByOrderCode(String orderCode);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    // Lọc đơn hàng theo trạng thái (dùng cho admin)
    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);

    boolean existsByOrderCode(String orderCode);
}