package com.example.fashionshop.exception;

import com.example.fashionshop.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===== Bắt AppException (lỗi nghiệp vụ) =====
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode.name(), ex.getMessage()));
    }

    // ===== Bắt lỗi validation (@Valid trong Controller) =====
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.ofValidation(errors));
    }

    // ===== Bắt lỗi chưa đăng nhập =====
    @ExceptionHandler({
            MultipartException.class,
            MaxUploadSizeExceededException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleUploadException(Exception ex) {
        log.warn("Upload error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        ErrorCode.INVALID_IMAGE_FILE.name(),
                        "File anh khong hop le, bi thieu truong file hoac vuot qua dung luong cho phep"
                ));
    }
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(ErrorCode.UNAUTHORIZED.name(), ErrorCode.UNAUTHORIZED.getMessage()));
    }

    // ===== Bắt lỗi không có quyền =====
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("AccessDenied: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(ErrorCode.FORBIDDEN.name(), ErrorCode.FORBIDDEN.getMessage()));
    }

    // ===== Bắt tất cả lỗi còn lại =====
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getClass().getName(), ex);  // thay System.err + printStackTrace

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        ErrorCode.INTERNAL_SERVER_ERROR.name(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
                ));
    }

    // ===== Inner class: Format response lỗi =====
    public record ErrorResponse(
            boolean success,
            String code,
            String message,
            Map<String, String> errors
    ) {
        static ErrorResponse of(String code, String message) {
            return new ErrorResponse(false, code, message, null);
        }

        static ErrorResponse ofValidation(Map<String, String> errors) {
            return new ErrorResponse(false,
                    ErrorCode.VALIDATION_ERROR.name(),
                    ErrorCode.VALIDATION_ERROR.getMessage(),
                    errors);
        }
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodNotSupported(
            HttpServletRequest request,
            HttpRequestMethodNotSupportedException ex
    ) {

        log.error("URL: {}", request.getRequestURI());
        log.error("Method: {}", request.getMethod());

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(ex.getMessage()));
    }
}
