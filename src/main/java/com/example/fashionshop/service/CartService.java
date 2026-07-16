package com.example.fashionshop.service;

import com.example.fashionshop.dto.cart.CartDto;
import com.example.fashionshop.entity.OrderItem;

import java.util.List;

public interface CartService {
    CartDto.Response getCart(String email);
    CartDto.Response addToCart(String email, CartDto.AddRequest request);
    CartDto.Response updateQuantity(String email, Long cartItemId, CartDto.UpdateRequest request);
    CartDto.Response removeItem(String email, Long cartItemId);
    void clearCart(String email);
    void restoreOrderItems(String email, List<OrderItem> orderItems);
}
