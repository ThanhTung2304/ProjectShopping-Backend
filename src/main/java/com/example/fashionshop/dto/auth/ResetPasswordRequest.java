package com.example.fashionshop.dto.auth;

import lombok.*;


@Getter
@Setter
public class ResetPasswordRequest {
    private String email;
    private String otp;
    private String newPassword;
}