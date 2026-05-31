package com.example.fashionshop.controller;

import com.example.fashionshop.dto.ApiResponse;
import com.example.fashionshop.dto.cart.CartDto;
import com.example.fashionshop.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // GET /api/cart
    // Lấy giỏ hàng của user đang đăng nhập
    @GetMapping
    public ResponseEntity<ApiResponse<CartDto.Response>> getCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.success(cartService.getCart(userDetails.getUsername())));
    }

    // POST /api/cart
    // Thêm sản phẩm vào giỏ hàng
    @PostMapping
    public ResponseEntity<ApiResponse<CartDto.Response>> addToCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CartDto.AddRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Thêm vào giỏ hàng thành công",
                        cartService.addToCart(userDetails.getUsername(), request)));
    }

    // PUT /api/cart/{id}
    // Cập nhật số lượng sản phẩm trong giỏ
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CartDto.Response>> updateQuantity(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody CartDto.UpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật giỏ hàng thành công",
                        cartService.updateQuantity(userDetails.getUsername(), id, request)));
    }

    // DELETE /api/cart/{id}
    // Xóa 1 sản phẩm khỏi giỏ
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<CartDto.Response>> removeItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Xóa sản phẩm khỏi giỏ thành công",
                        cartService.removeItem(userDetails.getUsername(), id)));
    }

    // DELETE /api/cart
    // Xóa toàn bộ giỏ hàng
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        cartService.clearCart(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Xóa giỏ hàng thành công"));
    }
}