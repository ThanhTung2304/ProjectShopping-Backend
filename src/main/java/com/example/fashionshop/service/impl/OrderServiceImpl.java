package com.example.fashionshop.service.impl;

import com.example.fashionshop.dto.order.OrderDto;
import com.example.fashionshop.entity.*;
import com.example.fashionshop.exception.AppException;
import com.example.fashionshop.exception.ErrorCode;
import com.example.fashionshop.mapper.OrderMapper;
import com.example.fashionshop.repository.*;
import com.example.fashionshop.service.CartService;
import com.example.fashionshop.service.CouponService;
import com.example.fashionshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final PaymentRepository paymentRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;
    private final CouponService couponService;
    private final CartService cartService;
    private final OrderMapper orderMapper;

    // ========================
    // Đặt hàng
    // ========================
    @Override
    @Transactional
    public OrderDto.Response placeOrder(String email, OrderDto.PlaceOrderRequest request) {
        User user = findUserByEmail(email);

        // 1. Lấy giỏ hàng
        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());
        if (cartItems.isEmpty()) {
            throw new AppException(ErrorCode.CART_EMPTY);
        }

        // 2. Lấy địa chỉ giao hàng
        Address address = addressRepository.findByIdAndUserId(request.getAddressId(), user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        // 3. Tính tổng tiền + kiểm tra tồn kho
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            ProductVariant variant = item.getVariant();
            if (variant.getStockQuantity() < item.getQuantity()) {
                throw new AppException(ErrorCode.VARIANT_NOT_ENOUGH_STOCK,
                        "Sản phẩm " + variant.getProduct().getName()
                                + " (" + variant.getSize() + "/" + variant.getColor()
                                + ") không đủ tồn kho");
            }
            BigDecimal price = variant.getSalePrice() != null
                    ? variant.getSalePrice() : variant.getPrice();
            totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // 4. Tính giảm giá từ coupon
        BigDecimal shippingFee = BigDecimal.valueOf(30000); // phí ship cố định
        BigDecimal discountAmount = BigDecimal.ZERO;
        Coupon coupon = null;

        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            coupon = couponService.validateCoupon(request.getCouponCode(), totalAmount);
            var applyResult = couponService.applyCoupon(request.getCouponCode(), totalAmount);
            discountAmount = applyResult.getDiscountAmount();
        }

        BigDecimal finalAmount = totalAmount.add(shippingFee).subtract(discountAmount);

        // 5. Tạo Order
        Order order = Order.builder()
                .user(user)
                .orderCode(generateOrderCode())
                .status(Order.OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .shippingName(address.getFullName())
                .shippingPhone(address.getPhone())
                .shippingAddress(address.getDetail() + ", " + address.getWard()
                        + ", " + address.getDistrict() + ", " + address.getProvince())
                .note(request.getNote())
                .build();

        orderRepository.save(order);

        // 6. Tạo OrderItems + trừ tồn kho
        for (CartItem item : cartItems) {
            ProductVariant variant = item.getVariant();
            BigDecimal price = variant.getSalePrice() != null
                    ? variant.getSalePrice() : variant.getPrice();

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .variant(variant)
                    .productName(variant.getProduct().getName())
                    .size(variant.getSize())
                    .color(variant.getColor())
                    .quantity(item.getQuantity())
                    .unitPrice(price)
                    .subtotal(price.multiply(BigDecimal.valueOf(item.getQuantity())))
                    .build();

            orderItemRepository.save(orderItem);

            // Trừ tồn kho
            variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
            variantRepository.save(variant);
        }

        // 7. Tạo Payment
        Payment payment = Payment.builder()
                .order(order)
                .method(request.getPaymentMethod())
                .status(Payment.PaymentStatus.PENDING)
                .amount(finalAmount)
                .build();

        paymentRepository.save(payment);

        // 8. Tăng lượt dùng coupon
        if (coupon != null) {
            couponService.incrementUsage(coupon.getId());
        }

        // 9. Xóa giỏ hàng
        cartService.clearCart(email);

        return buildOrderResponse(order, payment);
    }

    // ========================
    // Lịch sử đơn hàng của user
    // ========================
    @Override
    public Page<OrderDto.Summary> getMyOrders(String email, Pageable pageable) {
        User user = findUserByEmail(email);
        return orderRepository.findByUserId(user.getId(), pageable)
                .map(order -> {
                    Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
                    return buildOrderSummary(order, payment);
                });
    }

    // ========================
    // Chi tiết đơn hàng
    // ========================
    @Override
    public OrderDto.Response getOrderDetail(String email, Long orderId) {
        User user = findUserByEmail(email);
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        return buildOrderResponse(order, payment);
    }

    // ========================
    // Hủy đơn hàng
    // ========================
    @Override
    @Transactional
    public void cancelOrder(String email, Long orderId) {
        User user = findUserByEmail(email);
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Chỉ hủy được khi PENDING hoặc CONFIRMED
        if (order.getStatus() != Order.OrderStatus.PENDING
                && order.getStatus() != Order.OrderStatus.CONFIRMED) {
            throw new AppException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Hoàn lại tồn kho
        order.getOrderItems().forEach(item -> {
            ProductVariant variant = item.getVariant();
            variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            variantRepository.save(variant);
        });
    }

    // ========================
    // ADMIN: Lấy tất cả đơn hàng
    // ========================
    @Override
    public Page<OrderDto.Summary> getAllOrders(String keyword, String status, Pageable pageable) {
        Order.OrderStatus orderStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.VALIDATION_ERROR);
            }
        }

        return orderRepository.filterOrders(orderStatus, keyword, null, null, pageable)
                .map(order -> {
                    Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
                    return buildOrderSummary(order, payment);
                });
    }

    // ========================
    // ADMIN: Cập nhật trạng thái
    // ========================
    @Override
    @Transactional
    public OrderDto.Response updateStatus(Long orderId, OrderDto.UpdateStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        order.setStatus(request.getStatus());
        orderRepository.save(order);

        // Nếu DELIVERED → cập nhật Payment thành PAID (COD)
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment != null
                && request.getStatus() == Order.OrderStatus.DELIVERED
                && payment.getMethod() == Payment.PaymentMethod.COD) {
            payment.setStatus(Payment.PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
        }

        return buildOrderResponse(order, payment);
    }

    // ========================
    // Helpers
    // ========================
    private String generateOrderCode() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", new Random().nextInt(9999));
        String code = "ORD-" + date + "-" + random;

        // Đảm bảo không trùng
        if (orderRepository.existsByOrderCode(code)) {
            return generateOrderCode();
        }
        return code;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private OrderDto.Response buildOrderResponse(Order order, Payment payment) {
        OrderDto.Response response = orderMapper.toResponse(order);
        return OrderDto.Response.builder()
                .id(response.getId())
                .orderCode(response.getOrderCode())
                .status(response.getStatus())
                .totalAmount(response.getTotalAmount())
                .shippingFee(response.getShippingFee())
                .discountAmount(response.getDiscountAmount())
                .finalAmount(response.getFinalAmount())
                .shippingName(response.getShippingName())
                .shippingPhone(response.getShippingPhone())
                .shippingAddress(response.getShippingAddress())
                .note(response.getNote())
                .orderedAt(response.getOrderedAt())
                .items(response.getItems())
                .paymentMethod(payment != null ? payment.getMethod().name() : null)
                .paymentStatus(payment != null ? payment.getStatus().name() : null)
                .build();
    }

    private OrderDto.Summary buildOrderSummary(Order order, Payment payment) {
        return OrderDto.Summary.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .finalAmount(order.getFinalAmount())
                .totalItems(order.getOrderItems() != null ? order.getOrderItems().size() : 0)
                .orderedAt(order.getOrderedAt())
                .paymentMethod(payment != null ? payment.getMethod().name() : null)
                .paymentStatus(payment != null ? payment.getStatus().name() : null)
                .build();
    }
}