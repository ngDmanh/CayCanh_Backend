package com.caycanh.caycanh_backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Bước 1: khách nhập email để nhận OTP reset mật khẩu.
 */
public record ForgotPasswordRequest(
        @NotBlank @Email String email
) {}
