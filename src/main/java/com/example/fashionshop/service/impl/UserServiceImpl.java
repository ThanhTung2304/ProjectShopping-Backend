package com.example.fashionshop.service.impl;

import com.example.fashionshop.dto.user.UserDto;
import com.example.fashionshop.entity.User;
import com.example.fashionshop.exception.AppException;
import com.example.fashionshop.exception.ErrorCode;
import com.example.fashionshop.mapper.UserMapper;
import com.example.fashionshop.repository.UserRepository;
import com.example.fashionshop.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // ========================
    // Lấy thông tin profile
    // ========================
    @Override
    public UserDto.Response getProfile(String email) {
        User user = findByEmail(email);
        return userMapper.toResponse(user);
    }

    // ========================
    // Cập nhật profile
    // ========================
    @Override
    @Transactional
    public UserDto.Response updateProfile(String email, UserDto.UpdateRequest request) {
        User user = findByEmail(email);

        // Kiểm tra SĐT mới có bị trùng không (trừ chính mình)
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && !request.getPhone().equals(user.getPhone())
                && userRepository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());

        return userMapper.toResponse(userRepository.save(user));
    }

    // ========================
    // Đổi mật khẩu
    // ========================
    @Override
    @Transactional
    public void changePassword(String email, UserDto.ChangePasswordRequest request) {
        User user = findByEmail(email);

        // Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // ========================
    // Helper
    // ========================
    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}