package com.caycanh.caycanh_backend.service;

import com.caycanh.caycanh_backend.dto.MessageResponse;
import com.caycanh.caycanh_backend.dto.auth.AuthResponse;
import com.caycanh.caycanh_backend.dto.auth.LoginRequest;
import com.caycanh.caycanh_backend.dto.auth.RegisterRequest;
import com.caycanh.caycanh_backend.dto.auth.ResendOtpRequest;
import com.caycanh.caycanh_backend.dto.auth.VerifyEmailRequest;
import com.caycanh.caycanh_backend.entity.User;
import com.caycanh.caycanh_backend.repository.UserRepository;
import com.caycanh.caycanh_backend.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.caycanh.caycanh_backend.dto.auth.ForgotPasswordRequest;
import com.caycanh.caycanh_backend.dto.auth.VerifyResetOtpRequest;
import com.caycanh.caycanh_backend.dto.auth.VerifyResetOtpResponse;
import com.caycanh.caycanh_backend.dto.auth.ResetPasswordRequest;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final EmailService emailService;
    private final ResetTokenService resetTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, OtpService otpService, EmailService emailService,
                       ResetTokenService resetTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.otpService = otpService;
        this.emailService = emailService;
        this.resetTokenService = resetTokenService;
    }

    @Transactional
    public MessageResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        User user = User.builder()
                .fullName(req.fullName().trim())
                .email(email)
                .phone(req.phone())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(User.Role.customer)
                .isActive(false) // chờ xác thực OTP
                .build();

        userRepository.save(user);

        String otp = otpService.generateAndStore(email);
        emailService.sendOtp(email, otp);

        return new MessageResponse("Mã OTP đã được gửi đến " + email + ". Vui lòng kiểm tra email và xác thực tài khoản.");
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest req) {
        String email = req.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));

        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Tài khoản đã được xác thực");
        }

        if (!otpService.validate(email, req.otp())) {
            throw new IllegalArgumentException("OTP không hợp lệ hoặc đã hết hạn");
        }

        user.setIsActive(true);
        userRepository.save(user);

        String token = jwtUtil.generateAccessToken(user);
        return toAuthResponse(user, token);
    }

    @Transactional
    public MessageResponse resendOtp(ResendOtpRequest req) {
        String email = req.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));

        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Tài khoản đã được xác thực");
        }

        String otp = otpService.generateAndStore(email);
        emailService.sendOtp(email, otp);

        return new MessageResponse("Mã OTP mới đã được gửi đến " + email + ".");
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không đúng"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Tài khoản chưa được xác thực email. Vui lòng kiểm tra hộp thư.");
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không đúng");
        }

        String token = jwtUtil.generateAccessToken(user);
        return toAuthResponse(user, token);
    }

    // ── QUÊN MẬT KHẨU ──────────────────────────────────────────

    /**
     * Bước 1: khách yêu cầu reset mật khẩu.
     * Sinh OTP và gửi qua email NẾU email tồn tại.
     *
     * BẢO MẬT: luôn trả về cùng message dù email có tồn tại hay không
     * để tránh kẻ xấu dò danh sách email đã đăng ký.
     */
    @Transactional(readOnly = true)
    public MessageResponse forgotPassword(ForgotPasswordRequest req) {
        String email = req.email().trim().toLowerCase();

        userRepository.findByEmail(email).ifPresent(user -> {
            String otp = otpService.generateAndStore(email);
            emailService.sendPasswordResetOtp(email, otp);
        });

        return new MessageResponse(
                "Nếu email này đã đăng ký, mã OTP đặt lại mật khẩu đã được gửi. " +
                        "Vui lòng kiểm tra hộp thư."
        );
    }

    /**
     * Bước 2: verify OTP, nếu đúng sinh reset token.
     * Token có TTL 10 phút — khách dùng ở bước 3 để đặt mật khẩu mới.
     */
    @Transactional(readOnly = true)
    public VerifyResetOtpResponse verifyResetOtp(VerifyResetOtpRequest req) {
        String email = req.email().trim().toLowerCase();

        // Phải đảm bảo email tồn tại trước khi verify OTP
        // (tránh tạo token cho email không tồn tại)
        if (userRepository.findByEmail(email).isEmpty()) {
            throw new IllegalArgumentException("OTP không hợp lệ hoặc đã hết hạn");
        }

        if (!otpService.validate(email, req.otp())) {
            throw new IllegalArgumentException("OTP không hợp lệ hoặc đã hết hạn");
        }

        // OTP đúng → sinh reset token
        String token = resetTokenService.generateAndStore(email);

        return new VerifyResetOtpResponse(
                token,
                600,  // 10 phút
                "Xác thực thành công. Vui lòng đặt mật khẩu mới trong vòng 10 phút."
        );
    }

    /**
     * Bước 3: dùng reset token để đặt mật khẩu mới.
     * Token chỉ dùng 1 lần, tự xóa sau khi consume.
     */
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest req) {
        // Đổi token → email (token bị xóa luôn sau khi đọc)
        String email = resetTokenService.consume(req.resetToken())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Token không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu lại."));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy tài khoản"));

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);

        return new MessageResponse(
                "Đặt lại mật khẩu thành công. Bạn có thể đăng nhập với mật khẩu mới."
        );
    }

    private static AuthResponse toAuthResponse(User user, String token) {
        return new AuthResponse(
                token,
                new AuthResponse.UserMeResponse(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getPhone(),
                        user.getRole().name()
                )
        );
    }
}
