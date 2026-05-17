package com.caycanh.caycanh_backend.controller;

import com.caycanh.caycanh_backend.dto.MessageResponse;
import com.caycanh.caycanh_backend.dto.auth.AuthResponse;
import com.caycanh.caycanh_backend.dto.auth.LoginRequest;
import com.caycanh.caycanh_backend.dto.auth.RegisterRequest;
import com.caycanh.caycanh_backend.dto.auth.ResendOtpRequest;
import com.caycanh.caycanh_backend.dto.auth.VerifyEmailRequest;
import com.caycanh.caycanh_backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.caycanh.caycanh_backend.dto.auth.ForgotPasswordRequest;
import com.caycanh.caycanh_backend.dto.auth.VerifyResetOtpRequest;
import com.caycanh.caycanh_backend.dto.auth.VerifyResetOtpResponse;
import com.caycanh.caycanh_backend.dto.auth.ResetPasswordRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        return ResponseEntity.ok(authService.verifyEmail(req));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<MessageResponse> resendOtp(@Valid @RequestBody ResendOtpRequest req) {
        return ResponseEntity.ok(authService.resendOtp(req));
    }

    /** Bước 1: yêu cầu reset mật khẩu — gửi OTP qua email */
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest req
    ) {
        return ResponseEntity.ok(authService.forgotPassword(req));
    }

    /** Bước 2: verify OTP, trả về resetToken */
    @PostMapping("/verify-reset-otp")
    public ResponseEntity<VerifyResetOtpResponse> verifyResetOtp(
            @Valid @RequestBody VerifyResetOtpRequest req
    ) {
        return ResponseEntity.ok(authService.verifyResetOtp(req));
    }

    /** Bước 3: dùng resetToken để đặt mật khẩu mới */
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest req
    ) {
        return ResponseEntity.ok(authService.resetPassword(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }
}