package com.caycanh.caycanh_backend.controller;

import com.caycanh.caycanh_backend.dto.auth.AuthResponse;
import com.caycanh.caycanh_backend.dto.auth.UpdateProfileRequest;
import com.caycanh.caycanh_backend.entity.User;
import com.caycanh.caycanh_backend.repository.UserRepository;
import com.caycanh.caycanh_backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint dành cho user thao tác trên chính mình.
 * Không có class-level prefix để khớp đúng path /api/me.
 */
@RestController
public class UserController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public UserController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    /**
     * Khách tự cập nhật profile — đổi fullName + phone.
     * Email không thay đổi được qua endpoint này.
     */
    @PatchMapping("/api/me")
    public ResponseEntity<AuthResponse.UserMeResponse> updateMyProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest req
    ) {
        return ResponseEntity.ok(authService.updateMyProfile(user, req));
    }

    /**
     * Lấy profile của user hiện tại.
     * Phải reload từ DB vì @AuthenticationPrincipal trả về User
     * được build từ JWT — chỉ có sub/email/role, thiếu phone & các field khác.
     */
    @GetMapping("/api/me")
    public ResponseEntity<AuthResponse.UserMeResponse> getMyProfile(
            @AuthenticationPrincipal User principal
    ) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));

        return ResponseEntity.ok(new AuthResponse.UserMeResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.getLastProfileUpdatedAt()    // ← thêm
        ));
    }
}