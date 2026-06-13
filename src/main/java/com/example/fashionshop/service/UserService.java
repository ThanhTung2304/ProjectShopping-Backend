package com.example.fashionshop.service;

import com.example.fashionshop.dto.user.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserDto.Response getProfile(String email);
    UserDto.Response updateProfile(String email, UserDto.UpdateRequest request);
    void changePassword(String email, UserDto.ChangePasswordRequest request);
    Page<UserDto.Response> getAllUsers(Pageable pageable);
    UserDto.Response getUserById(Long id);
    UserDto.Response updateUser(Long id, UserDto.AdminUpdateRequest request);
    UserDto.Response updateUserStatus(Long id, UserDto.UpdateStatusRequest request);
    UserDto.Response updateUserRole(Long id, UserDto.UpdateRoleRequest request);
    void deleteUser(Long id);
    void hardDeleteUser(Long id);
}
