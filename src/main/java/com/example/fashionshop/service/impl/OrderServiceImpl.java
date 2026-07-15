package com.example.fashionshop.service.impl;

import com.example.fashionshop.dto.order.OrderDto;
import com.example.fashionshop.entity.*;
import com.example.fashionshop.entity.Notification.NotificationType;
import com.example.fashionshop.exception.AppException;
import com.example.fashionshop.exception.ErrorCode;
import com.example.fashionshop.mapper.OrderMapper;
import com.example.fashionshop.repository.*;
import com.example.fashionshop.service.CartService;
import com.example.fashionshop.service.CouponService;
import com.example.fashionshop.service.NotificationService;
import com.example.fashionshop.service.OrderService;
import com.example.fashionshop.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    // Dùng chung giờ Việt Nam cho mọi mốc thời gian liên quan đến VNPay/đơn hàng
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final int MAX_ORDER_CODE_ATTEMPTS = 3;

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
    private final NotificationService notificationService;
    private final VnPayService vnPayService;

    @Value("${app.order.shipping-fee:30000}")
    private BigDecimal shippingFee;

    // ========================
    // Đặt hàng
    // ========================
    @Override
    @Transactional
    public OrderDto.Response placeOrder(String email, OrderDto.PlaceOrderRequest request,
                                        HttpServletRequest httpRequest) {
        User user = findUserByEmail(email);

        // 1. Lấy giỏ hàng
        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());
        if (cartItems.isEmpty()) {
            throw new AppException(ErrorCode.CART_EMPTY);
        }

        // 2. Lấy địa chỉ giao hàng
        Address address = addressRepository.findByIdAndUserId(request.getAddressId(), user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        // 3. Tính tổng tiền
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            ProductVariant variant = item.getVariant();
            BigDecimal price = variant.getSalePrice() != null
                    ? variant.getSalePrice() : variant.getPrice();
            totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // 4. Tính giảm giá từ coupon
        BigDecimal discountAmount = BigDecimal.ZERO;
        Coupon coupon = null;

        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            coupon = couponService.validateCoupon(request.getCouponCode(), totalAmount);
            var applyResult = couponService.applyCoupon(request.getCouponCode(), totalAmount);
            discountAmount = applyResult.getDiscountAmount();
        }

        BigDecimal finalAmount = totalAmount.add(shippingFee).subtract(discountAmount);

        // 5. Tạo Order (mã đơn sinh an toàn dưới tải cao, có retry khi đụng độ)
        Order order = Order.builder()
                .user(user)
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

        saveOrderWithUniqueCode(order);

        // 6. Tạo OrderItems + trừ tồn kho (atomic, chống oversell)
        for (CartItem item : cartItems) {
            ProductVariant variant = item.getVariant();

            int updated = variantRepository.decreaseStock(variant.getId(), item.getQuantity());
            if (updated == 0) {
                throw new AppException(ErrorCode.VARIANT_NOT_ENOUGH_STOCK,
                        "Sản phẩm " + variant.getProduct().getName()
                                + " (" + variant.getSize() + "/" + variant.getColor()
                                + ") không đủ tồn kho");
            }

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
        }

        // 7. Tạo Payment
        Payment payment = Payment.builder()
                .order(order)
                .method(request.getPaymentMethod())
                .status(Payment.PaymentStatus.PENDING)
                .amount(finalAmount)
                .build();

        paymentRepository.save(payment);

        // 8. Tăng lượt dùng coupon (atomic, chống vượt quota)
        if (coupon != null) {
            couponService.incrementUsage(coupon.getId());
        }

        // 9. Xóa giỏ hàng
        cartService.clearCart(email);

        // 10. Nếu thanh toán VNPay, tạo payment URL
        String paymentUrl = null;
        if (request.getPaymentMethod() == Payment.PaymentMethod.VNPAY) {
            paymentUrl = vnPayService.createPaymentUrl(order, httpRequest);
        }

        return buildOrderResponse(order, payment, paymentUrl);
    }

    // ========================
    // Lịch sử đơn hàng của user
    // ========================
    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public OrderDto.Response getOrderDetail(String email, Long orderId) {
        User user = findUserByEmail(email);
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        return buildOrderResponse(order, payment, null);
    }

    // ========================
    // Hủy đơn hàng
    // ========================
    @Override
    @Transactional
    public void cancelOrder(String email, Long orderId) {
        User user = findUserByEmail(email);

        Order order = orderRepository.findByIdAndUserId(
                        orderId,
                        user.getId()
                )
                .orElseThrow(() ->
                        new AppException(ErrorCode.ORDER_NOT_FOUND)
                );

        if (order.getStatus() != Order.OrderStatus.PENDING
                && order.getStatus() != Order.OrderStatus.CONFIRMED) {
            throw new AppException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        // Khóa Payment trước khi thay đổi Order
        Payment payment = paymentRepository
                .findByOrderIdWithLock(orderId)
                .orElse(null);

        /*
         * Không cho hủy đơn đã thanh toán VNPay.
         * Nếu nghiệp vụ cho hủy, cần xây dựng quy trình REFUND riêng.
         */
        if (payment != null
                && payment.getMethod() == Payment.PaymentMethod.VNPAY
                && payment.getStatus() == Payment.PaymentStatus.PAID) {
            throw new AppException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        if (payment != null
                && payment.getStatus()
                == Payment.PaymentStatus.PENDING) {

            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }

        order.getOrderItems().forEach(item ->
                variantRepository.increaseStock(
                        item.getVariant().getId(),
                        item.getQuantity()
                )
        );

        cartService.restoreOrderItems(email, order.getOrderItems());

        notificationService.create(
                user,
                "Don hang da huy",
                "Don hang " + order.getOrderCode()
                        + " cua ban da duoc huy.",
                NotificationType.ORDER_CANCELLED,
                order.getId()
        );
    }

    // ========================
    // Khách xác nhận đã nhận hàng
    // ========================
    @Override
    @Transactional
    public void confirmReceived(String email, Long orderId) {
        User user = findUserByEmail(email);

        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != Order.OrderStatus.SHIPPING) {
            throw new AppException(ErrorCode.ORDER_INVALID_STATUS_TRANSITION);
        }

        Payment payment = paymentRepository
                .findByOrderIdWithLock(orderId)
                .orElse(null);

        order.setStatus(Order.OrderStatus.DELIVERED);
        orderRepository.save(order);

        if (payment != null
                && payment.getMethod() == Payment.PaymentMethod.COD
                && payment.getStatus() == Payment.PaymentStatus.PENDING) {

            payment.setStatus(Payment.PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now(VIETNAM_ZONE));
            paymentRepository.save(payment);
        }

        notificationService.create(
                user,
                "Da xac nhan nhan hang",
                "Don hang " + order.getOrderCode()
                        + " da duoc xac nhan la da giao thanh cong.",
                NotificationType.ORDER_DELIVERED,
                order.getId()
        );
    }

    // ========================
    // ADMIN: Lấy tất cả đơn hàng
    // ========================
    @Override
    @Transactional(readOnly = true)
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
    public OrderDto.Response updateStatus(
            Long orderId,
            OrderDto.UpdateStatusRequest request) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        validateAdminStatusTransition(
                order.getStatus(),
                request.getStatus()
        );

        Payment payment = paymentRepository
                .findByOrderIdWithLock(orderId)
                .orElse(null);

        if (request.getStatus() == Order.OrderStatus.CANCELLED
                && payment != null
                && payment.getMethod() == Payment.PaymentMethod.VNPAY
                && payment.getStatus() == Payment.PaymentStatus.PAID) {

            throw new AppException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        order.setStatus(request.getStatus());
        orderRepository.save(order);

        if (request.getStatus() == Order.OrderStatus.CANCELLED) {

            if (payment != null
                    && payment.getStatus() == Payment.PaymentStatus.PENDING) {

                payment.setStatus(Payment.PaymentStatus.FAILED);
                paymentRepository.save(payment);
            }

            order.getOrderItems().forEach(item ->
                    variantRepository.increaseStock(
                            item.getVariant().getId(),
                            item.getQuantity()
                    )
            );

            cartService.restoreOrderItems(order.getUser().getEmail(), order.getOrderItems());
        }

        if (request.getStatus() == Order.OrderStatus.DELIVERED
                && payment != null
                && payment.getMethod() == Payment.PaymentMethod.COD
                && payment.getStatus() == Payment.PaymentStatus.PENDING) {

            payment.setStatus(Payment.PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now(VIETNAM_ZONE));
            paymentRepository.save(payment);
        }

        notificationService.create(
                order.getUser(),
                "Trang thai don hang da cap nhat",
                "Don hang " + order.getOrderCode()
                        + " da chuyen sang trang thai "
                        + request.getStatus().name() + ".",
                NotificationType.ORDER_STATUS_UPDATED,
                order.getId()
        );

        return buildOrderResponse(order, payment, null);
    }

    private void validateAdminStatusTransition(Order.OrderStatus currentStatus, Order.OrderStatus nextStatus) {
        if (currentStatus == nextStatus) {
            return;
        }

        Set<Order.OrderStatus> allowedNextStatuses = switch (currentStatus) {
            case PENDING -> EnumSet.of(Order.OrderStatus.CONFIRMED, Order.OrderStatus.CANCELLED);
            case CONFIRMED -> EnumSet.of(Order.OrderStatus.SHIPPING, Order.OrderStatus.CANCELLED);
            case SHIPPING -> EnumSet.of(Order.OrderStatus.DELIVERED);
            case DELIVERED, CANCELLED -> EnumSet.noneOf(Order.OrderStatus.class);
        };

        if (!allowedNextStatuses.contains(nextStatus)) {
            throw new AppException(ErrorCode.ORDER_INVALID_STATUS_TRANSITION);
        }
    }

    // ========================
    // Helpers
    // ========================

    /**
     * Sinh mã đơn hàng dạng ORD-yyyyMMdd-XXXXXXXX (8 ký tự hex ngẫu nhiên).
     * Không cần check tồn tại trước khi insert (không atomic, có race condition);
     * thay vào đó dựa vào unique constraint của cột order_code và retry khi va chạm.
     */
    private String generateOrderCode() {
        String date = LocalDateTime.now(VIETNAM_ZONE)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
        return "ORD-" + date + "-" + random;
    }

    /**
     * Lưu Order với mã đơn duy nhất, retry tối đa MAX_ORDER_CODE_ATTEMPTS lần
     * nếu vi phạm unique constraint (order_code trùng - cực hiếm khi dùng UUID).
     */
    private void saveOrderWithUniqueCode(Order order) {
        int attempts = 0;
        while (true) {
            order.setOrderCode(generateOrderCode());
            try {
                orderRepository.save(order);
                orderRepository.flush();
                return;
            } catch (DataIntegrityViolationException e) {
                attempts++;
                if (attempts >= MAX_ORDER_CODE_ATTEMPTS) {
                    throw new AppException(ErrorCode.ORDER_CODE_EXISTS);
                }
            }
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private OrderDto.Response buildOrderResponse(Order order, Payment payment, String paymentUrl) {
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
                .paymentUrl(paymentUrl)
                .build();
    }

    private OrderDto.Summary buildOrderSummary(Order order, Payment payment) {
        return OrderDto.Summary.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .finalAmount(order.getFinalAmount())
                .totalItems(order.getOrderItems() != null ? order.getOrderItems().size() : 0)
                .orderedAt(order.getOrderedAt())
                .paymentMethod(payment != null ? payment.getMethod().name() : null)
                .paymentStatus(payment != null ? payment.getStatus().name() : null)
                .shippingName(order.getShippingName())
                .shippingPhone(order.getShippingPhone())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto.Response adminGetOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        return buildOrderResponse(order, payment, null);
    }
}
