package com.example.fashionshop.dto.auth;

import lombok.*;

@Getter
@Setter
public class VerifyOtpRequest {
    private String email;
    private String otp;
}