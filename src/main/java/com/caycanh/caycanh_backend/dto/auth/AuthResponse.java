package com.caycanh.caycanh_backend.dto.auth;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        UserMeResponse user
) {
    public record UserMeResponse(
            UUID id,
            String fullName,
            String email,
            String phone,
            String role
    ) {}
}

