package com.example.fashionshop.service;

import com.example.fashionshop.dto.auth.AuthResponse;
import com.example.fashionshop.dto.auth.LoginRequest;
import com.example.fashionshop.dto.auth.RegisterRequest;
import com.example.fashionshop.dto.auth.ResetPasswordRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);

    void forgotPassword(String email, OtpService otpService);
    void resetPassword(ResetPasswordRequest request, OtpService otpService);
}
