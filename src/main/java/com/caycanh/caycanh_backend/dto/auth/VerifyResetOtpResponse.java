package com.caycanh.caycanh_backend.dto.auth;

/**
 * Sau khi OTP đúng, trả token để khách dùng ở bước 3.
 * TTL 10 phút.
 */
public record VerifyResetOtpResponse(
        String resetToken,
        int expiresInSeconds,
        String message
) {}
