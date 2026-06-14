package com.example.fashionshop.controller;

import com.example.fashionshop.dto.ApiResponse;
import com.example.fashionshop.dto.product.ProductDto;
import com.example.fashionshop.dto.product.VariantDto;
import com.example.fashionshop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // GET /api/products
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductDto.Summary>>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String color,
            @PageableDefault(size = 12, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.getProducts(keyword, categoryId, minPrice, maxPrice, size, color, pageable)));
    }

    // GET /api/products/{slug}
    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<ProductDto.Response>> getProductBySlug(
            @PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductBySlug(slug)));
    }

    // GET /api/products/id/{id}
    @GetMapping("/id/{id}")
    public ResponseEntity<ApiResponse<ProductDto.Response>> getProductById(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    // POST /api/products — ADMIN only
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductDto.Response>> createProduct(
            @Valid @RequestBody ProductDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo sản phẩm thành công",
                        productService.createProduct(request)));
    }

    // PUT /api/products/{id} — ADMIN only
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductDto.Response>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductDto.Request request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công",
                productService.updateProduct(id, request)));
    }

    // DELETE /api/products/{id} — ADMIN only
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa sản phẩm thành công"));
    }

    // ===== VARIANT =====

    // GET /api/products/{productId}/variants — public
    @GetMapping("/{productId}/variants")
    public ResponseEntity<ApiResponse<List<VariantDto.Response>>> getVariantsByProduct(
            @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.getVariantsByProductId(productId)));
    }

    // POST /api/products/id/{productId}/variants — ADMIN only
    @PostMapping("/id/{productId}/variants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VariantDto.Response>> addVariant(
            @PathVariable Long productId,
            @Valid @RequestBody VariantDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm biến thể thành công",
                        productService.addVariant(productId, request)));
    }

    // PUT /api/products/variants/{variantId} — ADMIN only
    @PutMapping("/variants/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VariantDto.Response>> updateVariant(
            @PathVariable Long variantId,
            @Valid @RequestBody VariantDto.Request request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật biến thể thành công",
                productService.updateVariant(variantId, request)));
    }

    // DELETE /api/products/variants/{variantId} — ADMIN only
    @DeleteMapping("/variants/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(@PathVariable Long variantId) {
        productService.deleteVariant(variantId);
        return ResponseEntity.ok(ApiResponse.ok("Xóa biến thể thành công"));
    }
}