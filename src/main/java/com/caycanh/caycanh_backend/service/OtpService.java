package com.caycanh.caycanh_backend.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final int OTP_TTL_SECONDS = 300; // 5 phút
    private static final SecureRandom RANDOM = new SecureRandom();

    private record OtpEntry(String otp, Instant expiresAt) {}

    private final ConcurrentHashMap<String, OtpEntry> store = new ConcurrentHashMap<>();

    public String generateAndStore(String email) {
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        store.put(email.toLowerCase(), new OtpEntry(otp, Instant.now().plusSeconds(OTP_TTL_SECONDS)));
        return otp;
    }

    public boolean validate(String email, String otp) {
        String key = email.toLowerCase();
        OtpEntry entry = store.get(key);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            store.remove(key);
            return false;
        }
        if (!entry.otp().equals(otp)) return false;
        store.remove(key);
        return true;
    }

    public boolean hasActiveOtp(String email) {
        OtpEntry entry = store.get(email.toLowerCase());
        return entry != null && Instant.now().isBefore(entry.expiresAt());
    }
}
