package com.example.fashionshop.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ===== AUTH =====
    EMAIL_ALREADY_EXISTS        (HttpStatus.BAD_REQUEST,         "Email đã được sử dụng"),
    PHONE_ALREADY_EXISTS        (HttpStatus.BAD_REQUEST,         "Số điện thoại đã được sử dụng"),
    INVALID_CREDENTIALS         (HttpStatus.UNAUTHORIZED,        "Email hoặc mật khẩu không đúng"),
    ACCOUNT_DISABLED            (HttpStatus.FORBIDDEN,           "Tài khoản đã bị vô hiệu hóa"),
    UNAUTHORIZED                (HttpStatus.UNAUTHORIZED,        "Bạn chưa đăng nhập"),
    FORBIDDEN                   (HttpStatus.FORBIDDEN,           "Bạn không có quyền thực hiện thao tác này"),

    // ===== USER =====
    USER_NOT_FOUND              (HttpStatus.NOT_FOUND,           "Không tìm thấy người dùng"),

    // ===== ADDRESS =====
    ADDRESS_NOT_FOUND           (HttpStatus.NOT_FOUND,           "Không tìm thấy địa chỉ"),
    ADDRESS_NOT_BELONG_TO_USER  (HttpStatus.FORBIDDEN,           "Địa chỉ không thuộc về người dùng này"),

    // ===== CATEGORY =====
    CATEGORY_NOT_FOUND          (HttpStatus.NOT_FOUND,           "Không tìm thấy danh mục"),
    CATEGORY_SLUG_EXISTS        (HttpStatus.BAD_REQUEST,         "Slug danh mục đã tồn tại"),

    // ===== PRODUCT =====
    PRODUCT_NOT_FOUND           (HttpStatus.NOT_FOUND,           "Không tìm thấy sản phẩm"),
    PRODUCT_SLUG_EXISTS         (HttpStatus.BAD_REQUEST,         "Slug sản phẩm đã tồn tại"),

    // ===== PRODUCT VARIANT =====
    VARIANT_NOT_FOUND           (HttpStatus.NOT_FOUND,           "Không tìm thấy biến thể sản phẩm"),
    VARIANT_SKU_EXISTS          (HttpStatus.BAD_REQUEST,         "Mã SKU đã tồn tại"),
    VARIANT_OUT_OF_STOCK        (HttpStatus.BAD_REQUEST,         "Sản phẩm đã hết hàng"),
    VARIANT_NOT_ENOUGH_STOCK    (HttpStatus.BAD_REQUEST,         "Số lượng tồn kho không đủ"),

    // ===== CART =====
    CART_ITEM_NOT_FOUND         (HttpStatus.NOT_FOUND,           "Không tìm thấy sản phẩm trong giỏ hàng"),
    CART_EMPTY                  (HttpStatus.BAD_REQUEST,         "Giỏ hàng đang trống"),

    // ===== ORDER =====
    ORDER_NOT_FOUND             (HttpStatus.NOT_FOUND,           "Không tìm thấy đơn hàng"),
    ORDER_NOT_BELONG_TO_USER    (HttpStatus.FORBIDDEN,           "Đơn hàng không thuộc về người dùng này"),
    ORDER_CANNOT_CANCEL         (HttpStatus.BAD_REQUEST,         "Đơn hàng không thể hủy ở trạng thái hiện tại"),
    ORDER_INVALID_STATUS_TRANSITION (HttpStatus.BAD_REQUEST,      "Trạng thái đơn hàng không hợp lệ"),
    ORDER_CODE_EXISTS           (HttpStatus.INTERNAL_SERVER_ERROR, "Mã đơn hàng bị trùng, vui lòng thử lại"),

    // ===== PAYMENT =====
    PAYMENT_NOT_FOUND           (HttpStatus.NOT_FOUND,           "Không tìm thấy thông tin thanh toán"),
    PAYMENT_ALREADY_PAID        (HttpStatus.BAD_REQUEST,         "Đơn hàng đã được thanh toán"),

    // ===== REVIEW =====
    REVIEW_NOT_FOUND            (HttpStatus.NOT_FOUND,           "Không tìm thấy đánh giá"),
    REVIEW_ALREADY_EXISTS       (HttpStatus.BAD_REQUEST,         "Bạn đã đánh giá sản phẩm này"),
    REVIEW_NOT_PURCHASED        (HttpStatus.BAD_REQUEST,         "Bạn chưa mua sản phẩm này"),

    // ===== COUPON =====
    COUPON_NOT_FOUND            (HttpStatus.NOT_FOUND,           "Không tìm thấy mã giảm giá"),
    COUPON_INVALID              (HttpStatus.BAD_REQUEST,         "Mã giảm giá không hợp lệ hoặc đã hết hạn"),
    COUPON_CODE_EXISTS          (HttpStatus.BAD_REQUEST,         "Mã giảm giá đã tồn tại"),
    COUPON_MIN_ORDER_NOT_MET    (HttpStatus.BAD_REQUEST,         "Đơn hàng chưa đạt giá trị tối thiểu để dùng mã"),

    // ===== COMMON =====
    VALIDATION_ERROR            (HttpStatus.BAD_REQUEST,         "Dữ liệu không hợp lệ"),
    INTERNAL_SERVER_ERROR       (HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống, vui lòng thử lại sau");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
