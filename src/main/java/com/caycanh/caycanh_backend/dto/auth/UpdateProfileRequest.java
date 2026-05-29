package com.caycanh.caycanh_backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Khách cập nhật profile — chỉ đổi được fullName + phone.
 * Email không sửa được vì là khóa định danh + đã verify OTP.
 * Mật khẩu phải đổi qua flow forgot-password riêng.
 */
public record UpdateProfileRequest(
        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
        String fullName,

        @Pattern(
                regexp = "^0[0-9]{9,10}$",
                message = "Số điện thoại Việt Nam không hợp lệ (vd: 0912345678)"
        )
        String phone
) {}