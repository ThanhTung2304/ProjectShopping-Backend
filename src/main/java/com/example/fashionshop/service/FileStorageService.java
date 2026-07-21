package com.example.fashionshop.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.fashionshop.exception.AppException;
import com.example.fashionshop.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    // Matches .../upload/v123456/<public_id>.<ext> and captures the public_id
    private static final Pattern CLOUDINARY_PUBLIC_ID_PATTERN =
            Pattern.compile("/upload/(?:v\\d+/)?(.+)\\.[a-zA-Z0-9]+$");

    private final Cloudinary cloudinary;
    private final String uploadFolder;

    public FileStorageService(
            Cloudinary cloudinary,
            @Value("${app.upload.folder:leanh-studio}") String uploadFolder) {
        this.cloudinary = cloudinary;
        this.uploadFolder = uploadFolder;
    }

    public String storeProductImage(MultipartFile file) {
        return storeImage(file, "products");
    }

    public String storeCategoryImage(MultipartFile file) {
        return storeImage(file, "categories");
    }

    private String storeImage(MultipartFile file, String folderName) {
        validateImage(file);

        String extension = getExtension(file);
        String publicId = uploadFolder + "/" + folderName + "/" + UUID.randomUUID();

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "image",
                            "format", extension,
                            "overwrite", true
                    )
            );
            return (String) uploadResult.get("secure_url");
        } catch (IOException ex) {
            throw new AppException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    public void deleteByPublicUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        String publicId = extractPublicId(imageUrl);
        if (publicId == null) {
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (IOException ignored) {
            // Database state is the source of truth; failed remote cleanup should not block deletion.
        }
    }

    private String extractPublicId(String imageUrl) {
        Matcher matcher = CLOUDINARY_PUBLIC_ID_PATTERN.matcher(imageUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
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
}