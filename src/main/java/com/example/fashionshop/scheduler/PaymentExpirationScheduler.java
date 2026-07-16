package com.example.fashionshop.scheduler;

import com.example.fashionshop.entity.Order;
import com.example.fashionshop.entity.Payment;
import com.example.fashionshop.repository.OrderRepository;
import com.example.fashionshop.repository.PaymentRepository;
import com.example.fashionshop.repository.ProductVariantRepository;
import com.example.fashionshop.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentExpirationScheduler {

    private static final ZoneId VIETNAM_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final CartService cartService;

    /**
     * Chạy mỗi 15 phút.
     * Chỉ hủy Payment VNPay đang PENDING quá 30 phút.
     */
    @Scheduled(fixedDelay = 900000)
    @Transactional
    public void cancelExpiredPendingPayments() {

        LocalDateTime threshold =
                LocalDateTime.now(VIETNAM_ZONE).minusMinutes(30);

        /*
         * Query sử dụng PESSIMISTIC_WRITE.
         * Nếu IPN đang xử lý một payment thì scheduler phải chờ.
         */
        List<Payment> expiredPayments =
                paymentRepository.findExpiredPaymentsWithLock(
                        Payment.PaymentMethod.VNPAY,
                        Payment.PaymentStatus.PENDING,
                        threshold
                );

        for (Payment payment : expiredPayments) {

            /*
             * Kiểm tra lại trạng thái trong transaction.
             * Đây là lớp bảo vệ bổ sung.
             */
            if (payment.getStatus()
                    != Payment.PaymentStatus.PENDING) {
                continue;
            }

            Order order = payment.getOrder();

            /*
             * Chỉ hoàn kho khi đơn thực sự chưa bị hủy.
             * Tránh hoàn kho hai lần.
             */
            if (order.getStatus() == Order.OrderStatus.CANCELLED) {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                paymentRepository.save(payment);
                continue;
            }

            /*
             * Không tự động hủy đơn đã giao hoặc đang giao.
             */
            if (order.getStatus() != Order.OrderStatus.PENDING
                    && order.getStatus()
                    != Order.OrderStatus.CONFIRMED) {
                continue;
            }

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
}
