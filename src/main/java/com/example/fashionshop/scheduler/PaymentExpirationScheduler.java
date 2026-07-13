package com.example.fashionshop.scheduler;

import com.example.fashionshop.entity.Order;
import com.example.fashionshop.entity.Payment;
import com.example.fashionshop.repository.OrderRepository;
import com.example.fashionshop.repository.PaymentRepository;
import com.example.fashionshop.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentExpirationScheduler {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;

    // Chạy mỗi 15 phút, hủy Payment còn PENDING quá 30 phút
    @Scheduled(fixedRate = 900000)
    @Transactional
    public void cancelExpiredPendingPayments() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);

        List<Payment> expiredPayments = paymentRepository
                .findByMethodAndStatusAndOrderOrderedAtBefore(
                        Payment.PaymentMethod.VNPAY,
                        Payment.PaymentStatus.PENDING,
                        threshold
                );

        for (Payment payment : expiredPayments) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);

            Order order = payment.getOrder();
            order.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.save(order);

            order.getOrderItems().forEach(item ->
                    variantRepository.increaseStock(item.getVariant().getId(), item.getQuantity())
            );
        }
    }
}