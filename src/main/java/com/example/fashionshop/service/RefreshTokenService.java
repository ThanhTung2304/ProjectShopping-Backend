package com.example.fashionshop.service;

import com.example.fashionshop.entity.RefreshToken;
import com.example.fashionshop.entity.User;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user);
    RefreshToken verifyExpiration(RefreshToken token);
    RefreshToken findByToken(String token);
    void deleteByUser(User user);
}
