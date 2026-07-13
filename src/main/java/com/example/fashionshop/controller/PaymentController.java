package com.example.fashionshop.controller;

import com.example.fashionshop.entity.Order;
import com.example.fashionshop.entity.Payment;
import com.example.fashionshop.repository.PaymentRepository;
import com.example.fashionshop.service.VnPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment/vnpay")
@RequiredArgsConstructor
public class PaymentController {

    private final VnPayService vnPayService;
    private final PaymentRepository paymentRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // ========================
    // IPN — VNPay tự động gọi server-to-server, nguồn DUY NHẤT đáng tin cậy
    // ========================
    @GetMapping("/ipn")
    public ResponseEntity<Map<String, String>> handleIpn(@RequestParam Map<String, String> params) {

        if (!vnPayService.verifySignature(params)) {
            return vnpayResponse("97", "Invalid signature");
        }

        String orderCode = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");

        Payment payment = paymentRepository.findByOrderOrderCode(orderCode).orElse(null);

        if (payment == null) {
            return vnpayResponse("01", "Order not found");
        }

        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            return vnpayResponse("00", "Confirm Success");
        }

        if (payment.getOrder().getStatus() == Order.OrderStatus.CANCELLED) {
            return vnpayResponse("00", "Order cancelled");
        }

        if (payment.getMethod() != Payment.PaymentMethod.VNPAY) {
            return vnpayResponse("00", "Invalid payment method");
        }

        long vnpAmount = Long.parseLong(params.get("vnp_Amount")) / 100;

        if (payment.getAmount().longValue() != vnpAmount) {
            return vnpayResponse("04", "Invalid amount");
        }

        if ("00".equals(responseCode)) {
            payment.setStatus(Payment.PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            payment.setTransactionId(transactionNo);
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
        }
        paymentRepository.save(payment);

        return vnpayResponse("00", "Confirm Success");
    }

    // ========================
    // Return URL — khách quay lại sau khi thanh toán, CHỈ để hiển thị UI
    // ========================
    @GetMapping("/return")
    public ResponseEntity<Void> handleReturn(@RequestParam Map<String, String> params) {

        boolean valid = vnPayService.verifySignature(params);
        String orderCode = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");

        String redirectUrl = frontendUrl + "/order-result"
                + "?orderCode=" + orderCode
                + "&valid=" + valid
                + "&responseCode=" + responseCode;

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    private ResponseEntity<Map<String, String>> vnpayResponse(String code, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("RspCode", code);
        body.put("Message", message);
        return ResponseEntity.ok(body);
    }
}