package com.caycanh.caycanh_backend.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Khách bấm "Đặt hàng" - điền địa chỉ + người nhận + sđt + ghi chú.
 * Email tự lấy từ user đăng nhập, không cần gửi.
 */
public record CheckoutRequest(
        @NotBlank @Size(max = 100) String recipientName,

        @NotBlank
        @Pattern(regexp = "^(0[0-9]{9,10})$", message = "Số điện thoại không hợp lệ")
        String recipientPhone,

        @NotBlank @Size(max = 500) String shippingAddress,

        @Size(max = 500) String note
) {}