package com.example.fashionshop.controller;

import com.example.fashionshop.dto.ApiResponse;
import com.example.fashionshop.dto.product.ProductDto;
import com.example.fashionshop.dto.product.VariantDto;
import com.example.fashionshop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto.Response>> getProductById(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDto.Response>> createProduct(
            @Valid @RequestBody ProductDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tao san pham thanh cong",
                        productService.createProduct(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto.Response>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductDto.Request request) {
        return ResponseEntity.ok(ApiResponse.success("Cap nhat san pham thanh cong",
                productService.updateProduct(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.ok("Xoa san pham thanh cong"));
    }

    @PostMapping("/{productId}/variants")
    public ResponseEntity<ApiResponse<VariantDto.Response>> addVariant(
            @PathVariable Long productId,
            @Valid @RequestBody VariantDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Them bien the thanh cong",
                        productService.addVariant(productId, request)));
    }

    @PutMapping("/variants/{variantId}")
    public ResponseEntity<ApiResponse<VariantDto.Response>> updateVariant(
            @PathVariable Long variantId,
            @Valid @RequestBody VariantDto.Request request) {
        return ResponseEntity.ok(ApiResponse.success("Cap nhat bien the thanh cong",
                productService.updateVariant(variantId, request)));
    }

    @DeleteMapping("/variants/{variantId}")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(@PathVariable Long variantId) {
        productService.deleteVariant(variantId);
        return ResponseEntity.ok(ApiResponse.ok("Xoa bien the thanh cong"));
    }
}
