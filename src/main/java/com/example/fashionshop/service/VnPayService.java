package com.example.fashionshop.service;

import com.example.fashionshop.entity.Order;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface VnPayService {

    // Tạo payment URL để redirect khách sang trang thanh toán VNPay
    String createPaymentUrl(Order order, HttpServletRequest request);

    // Verify chữ ký từ VNPay gửi về (dùng cho cả IPN và Return URL)
    boolean verifySignature(Map<String, String> params);
}
