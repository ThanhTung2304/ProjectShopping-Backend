package com.example.fashionshop.controller;

import com.example.fashionshop.dto.ApiResponse;
import com.example.fashionshop.dto.product.ImageDto;
import com.example.fashionshop.dto.product.ProductDto;
import com.example.fashionshop.dto.product.VariantDto;
import com.example.fashionshop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    // ===== VARIANT =====

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

    // ===== IMAGE =====

    // GET /api/admin/products/{productId}/images
    @GetMapping("/{productId}/images")
    public ResponseEntity<ApiResponse<List<ImageDto.Response>>> getImages(
            @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.getProductImages(productId)));
    }

    // POST /api/admin/products/{productId}/images
    @PostMapping("/{productId}/images")
    public ResponseEntity<ApiResponse<ImageDto.Response>> uploadImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPrimary", defaultValue = "false") Boolean isPrimary,
            @RequestParam(value = "sortOrder", defaultValue = "0") Integer sortOrder) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Upload anh thanh cong",
                        productService.addProductImage(productId, file, isPrimary, sortOrder)));
    }

    // PUT /api/admin/products/{productId}/images/{imageId}/primary
    @PutMapping("/{productId}/images/{imageId}/primary")
    public ResponseEntity<ApiResponse<ImageDto.Response>> setPrimaryImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        return ResponseEntity.ok(ApiResponse.success("Da dat anh chinh",
                productService.setPrimaryProductImage(productId, imageId)));
    }

    // DELETE /api/admin/products/{productId}/images/{imageId}
    @DeleteMapping("/{productId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        productService.deleteProductImage(productId, imageId);
        return ResponseEntity.ok(ApiResponse.ok("Xoa anh thanh cong"));
    }
}