//MeController dùng để gom các API liên quan đến người dùng đang đăng nhập hiện tại.
//
//        Thay vì gọi kiểu cũ:
//
//        GET /api/users/profile
//        GET /api/addresses
//        GET /api/orders
//
//        route mới sẽ rõ nghĩa hơn:
//
//        GET /api/me
//        GET /api/me/addresses
//        GET /api/me/orders
//
//        Ý nghĩa của /api/me là: “thông tin của chính user đang đăng nhập”, lấy từ token JWT trong header:

package com.example.fashionshop.controller;

import com.example.fashionshop.dto.ApiResponse;
import com.example.fashionshop.dto.address.AddressDto;
import com.example.fashionshop.dto.order.OrderDto;
import com.example.fashionshop.dto.user.UserDto;
import com.example.fashionshop.service.AddressService;
import com.example.fashionshop.service.OrderService;
import com.example.fashionshop.service.UserService;
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

import java.util.List;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;
    private final AddressService addressService;
    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserDto.Response>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.success(userService.getProfile(userDetails.getUsername())));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserDto.Response>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserDto.UpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Cap nhat thanh cong",
                        userService.updateProfile(userDetails.getUsername(), request)));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserDto.ChangePasswordRequest request) {
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Doi mat khau thanh cong"));
    }

    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<List<AddressDto.Response>>> getAddresses(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.success(addressService.getAddresses(userDetails.getUsername())));
    }

    @PostMapping("/addresses")
    public ResponseEntity<ApiResponse<AddressDto.Response>> addAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddressDto.Request request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Them dia chi thanh cong",
                        addressService.addAddress(userDetails.getUsername(), request)));
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<ApiResponse<AddressDto.Response>> updateAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody AddressDto.Request request) {
        return ResponseEntity.ok(
                ApiResponse.success("Cap nhat dia chi thanh cong",
                        addressService.updateAddress(userDetails.getUsername(), id, request)));
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        addressService.deleteAddress(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Xoa dia chi thanh cong"));
    }

    @PatchMapping("/addresses/{id}/default")
    public ResponseEntity<ApiResponse<AddressDto.Response>> setDefaultAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Dat dia chi mac dinh thanh cong",
                        addressService.setDefault(userDetails.getUsername(), id)));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Page<OrderDto.Summary>>> getOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10, sort = "orderedAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getMyOrders(userDetails.getUsername(), pageable)));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderDto.Response>> getOrderDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getOrderDetail(userDetails.getUsername(), id)));
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody OrderDto.UpdateStatusRequest request) {
        orderService.updateMyOrderStatus(userDetails.getUsername(), id, request);
        return ResponseEntity.ok(ApiResponse.ok("Cap nhat trang thai don hang thanh cong"));
    }
}
