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

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, OtpService otpService, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.otpService = otpService;
        this.emailService = emailService;
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
