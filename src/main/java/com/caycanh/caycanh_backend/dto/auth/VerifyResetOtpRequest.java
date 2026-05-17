package com.caycanh.caycanh_backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Bước 2: app gửi kèm email (đã nhớ từ bước 1) + OTP khách vừa nhập.
 * Server verify, nếu OK trả về resetToken.
 */
public record VerifyResetOtpRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "OTP phải gồm 6 chữ số") String otp
) {}
