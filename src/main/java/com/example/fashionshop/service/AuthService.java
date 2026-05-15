package com.example.fashionshop.service;

import com.example.fashionshop.dto.auth.AuthResponse;
import com.example.fashionshop.dto.auth.LoginRequest;
import com.example.fashionshop.dto.auth.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
