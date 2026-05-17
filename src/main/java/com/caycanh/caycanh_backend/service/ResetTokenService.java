package com.caycanh.caycanh_backend.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý reset token tạm cho luồng quên mật khẩu.
 *
 * - Khi OTP verify thành công → sinh token, gắn với email
 * - Token chỉ dùng 1 lần, tự xóa sau khi reset password
 * - TTL 10 phút — đủ thời gian khách nhập mật khẩu mới
 *
 * Lưu RAM tương tự OtpService — đủ dùng cho quy mô hộ gia đình,
 * cần Redis nếu deploy nhiều instance.
 */
@Service
public class ResetTokenService {

    private static final int TOKEN_TTL_SECONDS = 600;  // 10 phút
    private static final int TOKEN_BYTES = 32;          // 256 bit
    private static final SecureRandom RANDOM = new SecureRandom();

    private record TokenEntry(String email, Instant expiresAt) {}

    /** Key = token, Value = (email + hạn) */
    private final ConcurrentHashMap<String, TokenEntry> store = new ConcurrentHashMap<>();

    /**
     * Sinh token mới gắn với email, lưu vào store, trả về cho client.
     */
    public String generateAndStore(String email) {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        store.put(token, new TokenEntry(
                email.toLowerCase(),
                Instant.now().plusSeconds(TOKEN_TTL_SECONDS)
        ));
        return token;
    }

    /**
     * Đổi token thành email (nếu token còn hợp lệ).
     * Token bị xóa luôn sau khi consume thành công — không tái sử dụng được.
     */
    public Optional<String> consume(String token) {
        TokenEntry entry = store.get(token);
        if (entry == null) return Optional.empty();
        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(token);
            return Optional.empty();
        }
        store.remove(token);  // xóa sau khi dùng
        return Optional.of(entry.email());
    }
}
