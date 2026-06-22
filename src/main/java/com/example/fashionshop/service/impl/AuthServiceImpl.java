package com.example.fashionshop.service.impl;

import com.example.fashionshop.config.JwtUtil;
import com.example.fashionshop.dto.auth.*;
import com.example.fashionshop.entity.RefreshToken;
import com.example.fashionshop.entity.User;
import com.example.fashionshop.exception.AppException;
import com.example.fashionshop.exception.ErrorCode;
import com.example.fashionshop.repository.UserRepository;
import com.example.fashionshop.service.AuthService;
import com.example.fashionshop.service.OtpService;
import com.example.fashionshop.service.RefreshTokenService;
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
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userRepository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(User.Role.CUSTOMER)
                .isActive(true)
                .build();

        userRepository.save(user);

        return createAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {

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

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!user.getIsActive()) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }

        return createAuthResponse(user);
    }

    @Override
    @Transactional
    public RefreshTokenResponse refreshAccessToken(String requestRefreshToken) {
        RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken);
        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        String newAccessToken = jwtUtil.generateToken(user.getEmail());

        return RefreshTokenResponse.builder()
                .token(newAccessToken)
                .build();
    }

    @Override
    @Transactional
    public void logout(String requestRefreshToken) {
        RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken);
        refreshTokenService.deleteByUser(refreshToken.getUser());
    }

    @Override
    public void forgotPassword(String email, OtpService otpService) {
        if (!userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        otpService.generateAndSend(email);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request, OtpService otpService) {
        if (!otpService.verifyOtp(request.getEmail(), request.getOtp())) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        otpService.clearOtp(request.getEmail());
    }

    private AuthResponse createAuthResponse(User user) {
        String token = jwtUtil.generateToken(user.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken.getToken())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

}
