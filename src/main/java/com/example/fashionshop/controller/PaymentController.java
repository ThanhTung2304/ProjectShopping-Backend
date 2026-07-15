package com.example.fashionshop.controller;

import com.example.fashionshop.dto.ApiResponse;
import com.example.fashionshop.entity.Notification;
import com.example.fashionshop.entity.Order;
import com.example.fashionshop.entity.Payment;
import com.example.fashionshop.exception.AppException;
import com.example.fashionshop.exception.ErrorCode;
import com.example.fashionshop.repository.OrderRepository;
import com.example.fashionshop.repository.PaymentRepository;
import com.example.fashionshop.repository.ProductVariantRepository;
import com.example.fashionshop.repository.UserRepository;
import com.example.fashionshop.service.CartService;
import com.example.fashionshop.service.NotificationService;
import com.example.fashionshop.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/vnpay")
@RequiredArgsConstructor
public class PaymentController {

    private static final ZoneId VIETNAM_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private static final DateTimeFormatter VNP_PAY_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnPayService vnPayService;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final NotificationService notificationService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Tạo URL thanh toán VNPay cho một đơn chưa được xử lý.
     * Đơn có phương thức BANK_TRANSFER sẽ được chuyển sang VNPAY khi tạo link.
     * Frontend gửi JSON: { "orderId": 123 } rồi chuyển hướng trình duyệt đến paymentUrl.
     */
    @PostMapping("/create")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody VnPayCreateRequest request,
            HttpServletRequest httpRequest) {

        Long userId = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND))
                .getId();

        Order order = orderRepository.findByIdAndUserId(request.orderId(), userId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        if ((payment.getMethod() != Payment.PaymentMethod.VNPAY
                && payment.getMethod() != Payment.PaymentMethod.BANK_TRANSFER)
                || payment.getStatus() != Payment.PaymentStatus.PENDING
                || order.getStatus() != Order.OrderStatus.PENDING) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Đơn hàng hiện không thể tạo liên kết thanh toán VNPay");
        }

        payment.setMethod(Payment.PaymentMethod.VNPAY);
        paymentRepository.save(payment);

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getId());
        data.put("orderCode", order.getOrderCode());
        data.put("paymentUrl", vnPayService.createPaymentUrl(order, httpRequest));
        data.put("expiresInMinutes", 15);

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // ========================
    // IPN — nguồn xác nhận thanh toán đáng tin cậy
    // ========================
    @GetMapping("/ipn")
    @Transactional
    public ResponseEntity<Map<String, String>> handleIpn(
            @RequestParam Map<String, String> params) {

        // 1. Kiểm tra chữ ký
        if (!vnPayService.verifySignature(params)) {
            return vnpayResponse("97", "Invalid signature");
        }

        String orderCode = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        String transactionNo = params.get("vnp_TransactionNo");
        String rawAmount = params.get("vnp_Amount");

        if (orderCode == null || orderCode.isBlank()) {
            return vnpayResponse("01", "Order not found");
        }

        /*
         * Khóa Payment cho đến khi transaction kết thúc.
         * Scheduler và thao tác hủy đơn phải chờ nếu đang xử lý cùng payment.
         */
        Payment payment = paymentRepository
                .findByOrderCodeWithLock(orderCode)
                .orElse(null);

        if (payment == null) {
            return vnpayResponse("01", "Order not found");
        }

        // 2. Chỉ xử lý payment VNPay
        if (payment.getMethod() != Payment.PaymentMethod.VNPAY) {
            return vnpayResponse("99", "Invalid payment method");
        }

        // 3. Payment đã được xử lý trước đó
        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            return vnpayResponse("02", "Order already confirmed");
        }

        Order order = payment.getOrder();

        // 4. Không nhận thanh toán cho đơn đã hủy
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);

            return vnpayResponse("02", "Order already cancelled");
        }

        // 5. Kiểm tra số tiền theo đơn vị VNPay (amount × 100)
        if (!isValidAmount(rawAmount, payment)) {
            return vnpayResponse("04", "Invalid amount");
        }

        /*
         * Chỉ thanh toán thành công khi cả hai mã đều là 00.
         */
        boolean paymentSuccessful =
                "00".equals(responseCode)
                        && "00".equals(transactionStatus);

        // Lưu thông tin đối soát bất kể thành công hay thất bại
        payment.setResponseCode(responseCode);
        payment.setTransactionStatus(transactionStatus);
        payment.setBankCode(params.get("vnp_BankCode"));
        payment.setPayDate(parseVnpPayDate(params.get("vnp_PayDate")));

        if (paymentSuccessful) {
            payment.setStatus(Payment.PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now(VIETNAM_ZONE));
            payment.setTransactionId(transactionNo);

            if (order.getStatus() == Order.OrderStatus.PENDING) {
                order.setStatus(Order.OrderStatus.CONFIRMED);
                orderRepository.save(order);
            }

            cartService.clearCart(order.getUser().getEmail());
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);

            /*
             * Thanh toán thất bại -> hủy đơn + hoàn kho NGAY LẬP TỨC.
             * Không đợi scheduler, vì scheduler chỉ xử lý trường hợp
             * không nhận được phản hồi nào từ VNPay (timeout thực sự).
             */
            if (order.getStatus() == Order.OrderStatus.PENDING) {
                order.setStatus(Order.OrderStatus.CANCELLED);
                orderRepository.save(order);

                order.getOrderItems().forEach(item ->
                        variantRepository.increaseStock(
                                item.getVariant().getId(),
                                item.getQuantity()
                        )
                );

                cartService.restoreOrderItems(order.getUser().getEmail(), order.getOrderItems());

                notificationService.create(
                        order.getUser(),
                        "Thanh toan khong thanh cong",
                        "Don hang " + order.getOrderCode()
                                + " thanh toan khong thanh cong va da duoc huy.",
                        Notification.NotificationType.ORDER_CANCELLED,
                        order.getId()
                );
            }
        }

        paymentRepository.save(payment);

        return vnpayResponse("00", "Confirm Success");
    }

    // ========================
    // Return URL — chỉ dùng để chuyển trình duyệt về frontend
    // Frontend KHÔNG được coi query param này là trạng thái thanh toán thật,
    // chỉ dùng để hiển thị màn hình chờ rồi gọi API lấy trạng thái đơn hàng.
    // ========================
    @GetMapping("/return")
    @Transactional
    public ResponseEntity<Void> handleReturn(
            @RequestParam Map<String, String> params) {

        boolean valid = vnPayService.verifySignature(params);

        String orderCode = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");

        boolean paymentSuccessful =
                valid
                        && "00".equals(responseCode)
                        && "00".equals(transactionStatus);

        if (!paymentSuccessful && valid && orderCode != null && !orderCode.isBlank()) {
            paymentRepository.findByOrderCodeWithLock(orderCode).ifPresent(payment -> {
                if (payment.getStatus() == Payment.PaymentStatus.PENDING) {
                    Order order = payment.getOrder();
                    if (order.getStatus() == Order.OrderStatus.PENDING
                            || order.getStatus() == Order.OrderStatus.CONFIRMED) {
                        payment.setStatus(Payment.PaymentStatus.FAILED);
                        order.setStatus(Order.OrderStatus.CANCELLED);
                        paymentRepository.save(payment);
                        orderRepository.save(order);

                        order.getOrderItems().forEach(item ->
                                variantRepository.increaseStock(
                                        item.getVariant().getId(),
                                        item.getQuantity()
                                )
                        );
                        cartService.restoreOrderItems(order.getUser().getEmail(), order.getOrderItems());
                    }
                }
            });
        }

        String redirectUrl = frontendUrl + "/order-result"
                + "?orderCode=" + encode(orderCode)
                + "&orderId=" + encode(resolveOrderId(orderCode))
                + "&valid=" + valid
                + "&responseCode=" + encode(responseCode)
                + "&transactionStatus=" + encode(transactionStatus);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    /**
     * VNPay gửi vnp_Amount bằng số tiền thực tế nhân 100.
     *
     * Ví dụ:
     * payment.amount = 150000.00
     * vnp_Amount = 15000000
     */
    private boolean isValidAmount(
            String rawAmount,
            Payment payment) {

        if (rawAmount == null || rawAmount.isBlank()) {
            return false;
        }

        try {
            long actualAmount = Long.parseLong(rawAmount);

            long expectedAmount = payment.getAmount()
                    .movePointRight(2)
                    .longValueExact();

            return actualAmount == expectedAmount;
        } catch (NumberFormatException | ArithmeticException exception) {
            return false;
        }
    }

    private LocalDateTime parseVnpPayDate(String vnpPayDate) {
        if (vnpPayDate == null || vnpPayDate.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(vnpPayDate, VNP_PAY_DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    private String encode(String value) {
        return value != null
                ? URLEncoder.encode(value, StandardCharsets.UTF_8)
                : "";
    }

    private String resolveOrderId(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) {
            return "";
        }
        return paymentRepository.findByOrderOrderCode(orderCode)
                .map(payment -> String.valueOf(payment.getOrder().getId()))
                .orElse("");
    }

    public record VnPayCreateRequest(@NotNull Long orderId) {
    }

    private ResponseEntity<Map<String, String>> vnpayResponse(
            String code,
            String message) {

        Map<String, String> body = new HashMap<>();
        body.put("RspCode", code);
        body.put("Message", message);

        return ResponseEntity.ok(body);
    }
}
