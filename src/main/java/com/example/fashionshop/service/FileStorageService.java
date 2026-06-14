package com.example.fashionshop.service;

import com.example.fashionshop.exception.AppException;
import com.example.fashionshop.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final Path uploadRoot;
    private final String publicPath;

    public FileStorageService(
            @Value("${app.upload.dir:uploads}") String uploadDir,
            @Value("${app.upload.public-path:/uploads}") String publicPath) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.publicPath = normalizePublicPath(publicPath);
    }

    public String storeProductImage(MultipartFile file) {
        validateImage(file);

        String extension = getExtension(file);
        String filename = UUID.randomUUID() + "." + extension;
        Path productDir = uploadRoot.resolve("products").normalize();
        Path target = productDir.resolve(filename).normalize();

        if (!target.startsWith(productDir)) {
            throw new AppException(ErrorCode.INVALID_IMAGE_FILE);
        }

        try {
            Files.createDirectories(productDir);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new AppException(ErrorCode.FILE_STORAGE_ERROR);
        }

        return publicPath + "/products/" + filename;
    }

    public void deleteByPublicUrl(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(publicPath + "/")) {
            return;
        }

        String relativePath = imageUrl.substring((publicPath + "/").length());
        Path target = uploadRoot.resolve(relativePath).normalize();

        if (!target.startsWith(uploadRoot)) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // Database state is the source of truth; failed file cleanup should not block deletion.
        }
    }

    public Path getUploadRoot() {
        return uploadRoot;
    }

    public String getPublicPath() {
        return publicPath;
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_IMAGE_FILE, "File anh khong duoc de trong");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new AppException(ErrorCode.INVALID_IMAGE_FILE, "Chi ho tro file jpg, jpeg, png, webp hoac gif");
        }

        String extension = getExtension(file);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new AppException(ErrorCode.INVALID_IMAGE_FILE, "Dinh dang file anh khong hop le");
        }
    }

    private String getExtension(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            throw new AppException(ErrorCode.INVALID_IMAGE_FILE, "File anh phai co phan mo rong");
        }
        return originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizePublicPath(String value) {
        String normalized = value == null || value.isBlank() ? "/uploads" : value.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
