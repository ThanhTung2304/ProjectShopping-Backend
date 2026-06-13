package com.example.fashionshop.service.impl;

import com.example.fashionshop.dto.user.UserDto;
import com.example.fashionshop.entity.User;
import com.example.fashionshop.exception.AppException;
import com.example.fashionshop.exception.ErrorCode;
import com.example.fashionshop.mapper.UserMapper;
import com.example.fashionshop.repository.OrderRepository;
import com.example.fashionshop.repository.ReviewRepository;
import com.example.fashionshop.repository.UserRepository;
import com.example.fashionshop.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
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

    @Override
    public Page<UserDto.Response> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toResponse);
    }

    @Override
    public UserDto.Response getUserById(Long id) {
        return userMapper.toResponse(findById(id));
    }

    @Override
    @Transactional
    public UserDto.Response updateUser(Long id, UserDto.AdminUpdateRequest request) {
        User user = findById(id);

        if (request.getFullName() != null) {
            if (request.getFullName().isBlank()) {
                throw new AppException(ErrorCode.VALIDATION_ERROR);
            }
            user.setFullName(request.getFullName());
        }

        if (request.getEmail() != null) {
            if (request.getEmail().isBlank()) {
                throw new AppException(ErrorCode.VALIDATION_ERROR);
            }
            String email = request.getEmail().trim();
            if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailAndIdNot(email, id)) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            user.setEmail(email);
        }

        if (request.getPhone() != null) {
            String phone = request.getPhone().trim();
            if (!phone.isBlank()
                    && !phone.equals(user.getPhone())
                    && userRepository.existsByPhoneAndIdNot(phone, id)) {
                throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
            user.setPhone(phone.isBlank() ? null : phone);
        }

        if (request.getRole() != null) {
            user.setRole(parseRole(request.getRole()));
        }

        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto.Response updateUserStatus(Long id, UserDto.UpdateStatusRequest request) {
        User user = findById(id);
        user.setIsActive(request.getIsActive());
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto.Response updateUserRole(Long id, UserDto.UpdateRoleRequest request) {
        User user = findById(id);
        user.setRole(parseRole(request.getRole()));
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = findById(id);
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void hardDeleteUser(Long id) {
        User user = findById(id);

        if (orderRepository.existsByUserId(id) || reviewRepository.existsByUserId(id)) {
            throw new AppException(ErrorCode.USER_CANNOT_HARD_DELETE);
        }

        userRepository.delete(user);
    }

    // ========================
    // Helper
    // ========================
    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private User.Role parseRole(String role) {
        try {
            return User.Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
