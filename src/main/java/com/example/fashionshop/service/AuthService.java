package com.example.fashionshop.service;

import com.example.fashionshop.dto.auth.*;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);

    RefreshTokenResponse refreshAccessToken(String refreshToken);
    void logout(String refreshToken);

    void forgotPassword(String email, OtpService otpService);
    void resetPassword(ResetPasswordRequest request, OtpService otpService);
}
