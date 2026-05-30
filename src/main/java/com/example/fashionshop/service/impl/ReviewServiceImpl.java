package com.example.fashionshop.service.impl;

import com.example.fashionshop.dto.UpdateStatusRequest.review.ReviewDto;
import com.example.fashionshop.entity.Order;
import com.example.fashionshop.entity.Product;
import com.example.fashionshop.entity.Review;
import com.example.fashionshop.entity.User;
import com.example.fashionshop.exception.AppException;
import com.example.fashionshop.exception.ErrorCode;
import com.example.fashionshop.mapper.ReviewMapper;
import com.example.fashionshop.repository.OrderRepository;
import com.example.fashionshop.repository.ProductRepository;
import com.example.fashionshop.repository.ReviewRepository;
import com.example.fashionshop.repository.UserRepository;
import com.example.fashionshop.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ReviewMapper reviewMapper;

    // ========================
    // Danh sách đánh giá của sản phẩm
    // ========================
    @Override
    public Page<ReviewDto.Response> getProductReviews(Long productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable)
                .map(reviewMapper::toResponse);
    }

    // ========================
    // Tổng hợp đánh giá (rating trung bình + phân bổ sao)
    // ========================
    @Override
    public ReviewDto.Summary getProductReviewSummary(Long productId) {
        Double avg = reviewRepository.findAverageRatingByProductId(productId);
        var ratingCounts = reviewRepository.countReviewsByRating(productId);

        Map<Integer, Long> distribution = new HashMap<>();
        for (Object[] row : ratingCounts) {
            distribution.put(((Number) row[0]).intValue(), (Long) row[1]);
        }

        int total = distribution.values().stream().mapToInt(Long::intValue).sum();

        return ReviewDto.Summary.builder()
                .averageRating(avg != null ? avg : 0.0)
                .totalReviews(total)
                .ratingDistribution(distribution)
                .build();
    }

    // ========================
    // Viết đánh giá
    // ========================
    @Override
    @Transactional
    public ReviewDto.Response addReview(String email, ReviewDto.Request request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Kiểm tra đơn hàng có thuộc user không
        if (!order.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.ORDER_NOT_BELONG_TO_USER);
        }

        // Kiểm tra đơn hàng đã DELIVERED chưa
        if (order.getStatus() != Order.OrderStatus.DELIVERED) {
            throw new AppException(ErrorCode.REVIEW_NOT_PURCHASED);
        }

        // Kiểm tra đã review chưa
        if (reviewRepository.existsByUserIdAndProductIdAndOrderId(
                user.getId(), product.getId(), order.getId())) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .order(order)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        return reviewMapper.toResponse(reviewRepository.save(review));
    }
}