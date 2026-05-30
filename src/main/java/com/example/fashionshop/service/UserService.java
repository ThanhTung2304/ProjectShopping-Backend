package com.example.fashionshop.service;

import com.example.fashionshop.dto.user.UserDto;

public interface UserService {
    UserDto.Response getProfile(String email);
    UserDto.Response updateProfile(String email, UserDto.UpdateRequest request);
    void changePassword(String email, UserDto.ChangePasswordRequest request);
}
