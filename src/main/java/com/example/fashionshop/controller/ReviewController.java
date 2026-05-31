package com.example.fashionshop.controller;

import com.example.fashionshop.dto.ApiResponse;
import com.example.fashionshop.dto.UpdateStatusRequest.review.ReviewDto;
import com.example.fashionshop.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // GET /api/reviews/product/{productId}
    // Danh sách đánh giá của sản phẩm (public)
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<Page<ReviewDto.Response>>> getProductReviews(
            @PathVariable Long productId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getProductReviews(productId, pageable)));
    }

    // GET /api/reviews/product/{productId}/summary
    // Tổng hợp đánh giá (rating trung bình + phân bổ sao)
    @GetMapping("/product/{productId}/summary")
    public ResponseEntity<ApiResponse<ReviewDto.Summary>> getProductReviewSummary(
            @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getProductReviewSummary(productId)));
    }

    // POST /api/reviews
    // Viết đánh giá (phải đăng nhập + đã mua hàng)
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewDto.Response>> addReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReviewDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đánh giá thành công",
                        reviewService.addReview(userDetails.getUsername(), request)));
    }
}