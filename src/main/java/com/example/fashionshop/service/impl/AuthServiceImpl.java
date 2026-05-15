package com.example.fashionshop.service.impl;

import com.example.fashionshop.config.JwtUtil;
import com.example.fashionshop.dto.auth.AuthResponse;
import com.example.fashionshop.dto.auth.LoginRequest;
import com.example.fashionshop.dto.auth.RegisterRequest;
import com.example.fashionshop.entity.User;
import com.example.fashionshop.exception.AppException;
import com.example.fashionshop.exception.ErrorCode;
import com.example.fashionshop.repository.UserRepository;
import com.example.fashionshop.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    // ========================
    // ĐĂNG KÝ
    // ========================
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // 1. Kiểm tra email đã tồn tại chưa
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 2. Kiểm tra SĐT đã tồn tại chưa (nếu có nhập)
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userRepository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        // 3. Tạo user mới
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // Hash password
                .phone(request.getPhone())
                .role(User.Role.CUSTOMER) // Mặc định là CUSTOMER
                .isActive(true)
                .build();

        userRepository.save(user);

        // 4. Tạo token và trả về
        String token = jwtUtil.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    // ========================
    // ĐĂNG NHẬP
    // ========================
    @Override
    public AuthResponse login(LoginRequest request) {

        // 1. Xác thực email + password
        // Spring Security tự so sánh password BCrypt
        // Nếu sai → throw BadCredentialsException
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 2. Lấy thông tin user từ DB
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 3. Kiểm tra tài khoản có bị khóa không
        if (!user.getIsActive()) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 4. Tạo token và trả về
        String token = jwtUtil.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }
}