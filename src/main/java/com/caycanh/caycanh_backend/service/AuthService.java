package com.caycanh.caycanh_backend.service;

import com.caycanh.caycanh_backend.dto.auth.AuthResponse;
import com.caycanh.caycanh_backend.dto.auth.LoginRequest;
import com.caycanh.caycanh_backend.dto.auth.RegisterRequest;
import com.caycanh.caycanh_backend.entity.User;
import com.caycanh.caycanh_backend.repository.UserRepository;
import com.caycanh.caycanh_backend.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    @SuppressWarnings("NullableProblems")
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .fullName(req.fullName().trim())
                .email(email)
                .phone(req.phone())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(User.Role.customer)
                .isActive(true)
                .build();

        User saved = Objects.requireNonNull(userRepository.save(user));
        @SuppressWarnings("DataFlowIssue")
        String token = jwtUtil.generateAccessToken(saved);
        return toAuthResponse(saved, token);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Account is inactive");
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
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

