// ========================
// Wrapper chung cho tất cả response thành công
// ========================
// Giúp response nhất quán:
// { "success": true, "message": "...", "data": {...} }

package com.example.fashionshop.dto;

import lombok.*;

@Getter @Setter
@Builder
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    //Thành công có data
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .data(data)
                .build();
    }

    //Thành cng có data + message tùy chỉnh
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    // Thành công không có data (vd: xóa)
    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .data(null)
                .build();
    }
}
