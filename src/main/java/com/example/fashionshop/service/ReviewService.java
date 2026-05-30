package com.example.fashionshop.service;

import com.example.fashionshop.dto.UpdateStatusRequest.review.ReviewDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    Page<ReviewDto.Response> getProductReviews(Long productId, Pageable pageable);
    ReviewDto.Summary getProductReviewSummary(Long productId);
    ReviewDto.Response addReview(String email, ReviewDto.Request request);
}