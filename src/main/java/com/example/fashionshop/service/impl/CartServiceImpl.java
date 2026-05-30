package com.example.fashionshop.service.impl;

import com.example.fashionshop.dto.cart.CartDto;
import com.example.fashionshop.entity.CartItem;
import com.example.fashionshop.entity.ProductVariant;
import com.example.fashionshop.entity.User;
import com.example.fashionshop.exception.AppException;
import com.example.fashionshop.exception.ErrorCode;
import com.example.fashionshop.repository.CartItemRepository;
import com.example.fashionshop.repository.ProductVariantRepository;
import com.example.fashionshop.repository.UserRepository;
import com.example.fashionshop.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;

    // ========================
    // Lấy giỏ hàng
    // ========================
    @Override
    public CartDto.Response getCart(String email) {
        User user = findUserByEmail(email);
        List<CartItem> items = cartItemRepository.findByUserId(user.getId());
        return buildCartResponse(items);
    }

    // ========================
    // Thêm vào giỏ hàng
    // ========================
    @Override
    @Transactional
    public CartDto.Response addToCart(String email, CartDto.AddRequest request) {
        User user = findUserByEmail(email);
        ProductVariant variant = findVariant(request.getVariantId());

        // Kiểm tra tồn kho
        if (variant.getStockQuantity() <= 0) {
            throw new AppException(ErrorCode.VARIANT_OUT_OF_STOCK);
        }

        // Nếu variant đã có trong giỏ → tăng quantity
        cartItemRepository.findByUserIdAndVariantId(user.getId(), variant.getId())
                .ifPresentOrElse(
                        existing -> {
                            int newQty = existing.getQuantity() + request.getQuantity();
                            if (newQty > variant.getStockQuantity()) {
                                throw new AppException(ErrorCode.VARIANT_NOT_ENOUGH_STOCK);
                            }
                            existing.setQuantity(newQty);
                            cartItemRepository.save(existing);
                        },
                        () -> {
                            if (request.getQuantity() > variant.getStockQuantity()) {
                                throw new AppException(ErrorCode.VARIANT_NOT_ENOUGH_STOCK);
                            }
                            CartItem newItem = CartItem.builder()
                                    .user(user)
                                    .variant(variant)
                                    .quantity(request.getQuantity())
                                    .build();
                            cartItemRepository.save(newItem);
                        }
                );

        return getCart(email);
    }

    // ========================
    // Cập nhật số lượng
    // ========================
    @Override
    @Transactional
    public CartDto.Response updateQuantity(String email, Long cartItemId, CartDto.UpdateRequest request) {
        User user = findUserByEmail(email);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        // Kiểm tra item có thuộc user không
        if (!item.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        // Kiểm tra tồn kho
        if (request.getQuantity() > item.getVariant().getStockQuantity()) {
            throw new AppException(ErrorCode.VARIANT_NOT_ENOUGH_STOCK);
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);
        return getCart(email);
    }

    // ========================
    // Xóa 1 item khỏi giỏ
    // ========================
    @Override
    @Transactional
    public CartDto.Response removeItem(String email, Long cartItemId) {
        User user = findUserByEmail(email);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (!item.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        cartItemRepository.delete(item);
        return getCart(email);
    }

    // ========================
    // Xóa toàn bộ giỏ hàng
    // ========================
    @Override
    @Transactional
    public void clearCart(String email) {
        User user = findUserByEmail(email);
        cartItemRepository.deleteAllByUserId(user.getId());
    }

    // ========================
    // Helper: Build CartResponse
    // ========================
    private CartDto.Response buildCartResponse(List<CartItem> items) {
        List<CartDto.ItemResponse> itemResponses = items.stream().map(item -> {
            ProductVariant variant = item.getVariant();
            BigDecimal unitPrice = variant.getSalePrice() != null
                    ? variant.getSalePrice() : variant.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            // Lấy ảnh đại diện sản phẩm
            String imageUrl = variant.getProduct().getImages().stream()
                    .filter(img -> img.getIsPrimary())
                    .map(img -> img.getImageUrl())
                    .findFirst().orElse(null);

            return CartDto.ItemResponse.builder()
                    .id(item.getId())
                    .variantId(variant.getId())
                    .productName(variant.getProduct().getName())
                    .size(variant.getSize())
                    .color(variant.getColor())
                    .imageUrl(imageUrl)
                    .unitPrice(unitPrice)
                    .quantity(item.getQuantity())
                    .subtotal(subtotal)
                    .stockQuantity(variant.getStockQuantity())
                    .build();
        }).toList();

        BigDecimal totalAmount = itemResponses.stream()
                .map(CartDto.ItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalQuantity = itemResponses.stream()
                .mapToInt(CartDto.ItemResponse::getQuantity).sum();

        return CartDto.Response.builder()
                .items(itemResponses)
                .totalItems(itemResponses.size())
                .totalQuantity(totalQuantity)
                .totalAmount(totalAmount)
                .build();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private ProductVariant findVariant(Long variantId) {
        return variantRepository.findById(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.VARIANT_NOT_FOUND));
    }
}