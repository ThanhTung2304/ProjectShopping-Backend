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
public class ApiPesponse<T> {
    private boolean success;
    private String message;
    private T data;

    //Thành công có data
    public static <T> ApiPesponse<T> success(T data) {
        return ApiPesponse.<T>builder()
                .success(true)
                .message("Success")
                .data(data)
                .build();
    }

    //Thành cng có data + message tùy chỉnh
    public static <T> ApiPesponse<T> success(String message, T data) {
        return ApiPesponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    // Thành công không có data (vd: xóa)
    public static <T> ApiPesponse<T> success() {
        return ApiPesponse.<T>builder()
                .success(true)
                .message("Success")
                .data(null)
                .build();
    }
}
