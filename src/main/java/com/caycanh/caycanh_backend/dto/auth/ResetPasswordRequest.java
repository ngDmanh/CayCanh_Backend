package com.caycanh.caycanh_backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Bước 3: dùng resetToken (đã nhận ở bước 2) + mật khẩu mới.
 * Không cần email — server tự lấy từ token.
 */
public record ResetPasswordRequest(
        @NotBlank String resetToken,
        @NotBlank @Size(min = 6, max = 100) String newPassword
) {}
