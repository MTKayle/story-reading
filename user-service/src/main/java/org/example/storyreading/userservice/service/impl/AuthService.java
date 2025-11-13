package org.example.storyreading.userservice.service.impl;

import org.example.storyreading.userservice.dto.AuthDtos;
import org.example.storyreading.userservice.entity.RefreshTokenEntity;
import org.example.storyreading.userservice.entity.RoleEntity;
import org.example.storyreading.userservice.entity.UserEntity;
import org.example.storyreading.userservice.repository.RefreshTokenRepository;
import org.example.storyreading.userservice.repository.RoleRepository;
import org.example.storyreading.userservice.repository.UserRepository;
import org.example.storyreading.userservice.security.JwtUtils;
import org.example.storyreading.userservice.service.IAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService implements IAuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Value("${refresh.expiration.minutes:43200}") // mặc định 30 ngày
    private long refreshExpirationMinutes;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Override
    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByUsername(request.username)) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }
        if (userRepository.existsByEmail(request.email)) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }

        RoleEntity role = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    RoleEntity r = new RoleEntity();
                    r.setName("USER");
                    return roleRepository.save(r);
                });

        UserEntity user = new UserEntity();
        user.setUsername(request.username);
        user.setEmail(request.email);
        user.setPassword(passwordEncoder.encode(request.password));
        user.setRole(role);
        user = userRepository.save(user);

        String accessToken = jwtUtils.generateToken(user.getId(), user.getUsername(), role.getName(), user.getEmail());
        String refreshToken = issueOrRotateRefreshToken(user);
        return new AuthDtos.AuthResponse(accessToken, refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        Optional<UserEntity> userOpt = userRepository.findByUsernameOrEmail(request.usernameOrEmail, request.usernameOrEmail);
        UserEntity user = userOpt.orElseThrow(() -> new IllegalArgumentException("Sai thông tin đăng nhập"));

        if (!passwordEncoder.matches(request.password, user.getPassword())) {
            throw new IllegalArgumentException("Sai thông tin đăng nhập");
        }

        String accessToken = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole().getName(), user.getEmail());
        String refreshToken = ensureRefreshToken(user);
        return new AuthDtos.AuthResponse(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthDtos.AuthResponse refresh(AuthDtos.RefreshTokenRequest request) {
        RefreshTokenEntity token = refreshTokenRepository.findByToken(request.refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token không hợp lệ. Vui lòng đăng nhập lại."));

        if (token.getExpiryDate().isBefore(Instant.now())) {
            // Xóa refresh token hết hạn để bảo mật
            refreshTokenRepository.delete(token);
            throw new IllegalArgumentException("Refresh token đã hết hạn. Vui lòng đăng nhập lại.");
        }

        UserEntity user = token.getUser();
        String accessToken = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole().getName(), user.getEmail());

        // Rotate refresh token để tăng bảo mật
        String newRefresh = rotateRefreshToken(user);
        return new AuthDtos.AuthResponse(accessToken, newRefresh);
    }

    private String ensureRefreshToken(UserEntity user) {
        return refreshTokenRepository.findByUser(user)
                .map(rt -> {
                    if (rt.getExpiryDate().isBefore(Instant.now())) {
                        // Refresh token hết hạn, xóa và tạo mới (user đang đăng nhập lại)
                        refreshTokenRepository.delete(rt);
                        return issueOrRotateRefreshToken(user);
                    }
                    // Refresh token còn hạn, giữ nguyên
                    return rt.getToken();
                })
                .orElseGet(() -> issueOrRotateRefreshToken(user));
    }

    private String issueOrRotateRefreshToken(UserEntity user) {
        RefreshTokenEntity rt = refreshTokenRepository.findByUser(user).orElse(new RefreshTokenEntity());
        rt.setUser(user);
        rt.setToken(UUID.randomUUID().toString());
        rt.setExpiryDate(Instant.now().plus(refreshExpirationMinutes, ChronoUnit.MINUTES));
        refreshTokenRepository.save(rt);
        return rt.getToken();
    }

    private String rotateRefreshToken(UserEntity user) {
        RefreshTokenEntity rt = refreshTokenRepository.findByUser(user)
                .orElseGet(() -> {
                    RefreshTokenEntity x = new RefreshTokenEntity();
                    x.setUser(user);
                    return x;
                });
        rt.setToken(UUID.randomUUID().toString());
        rt.setExpiryDate(Instant.now().plus(refreshExpirationMinutes, ChronoUnit.MINUTES));
        refreshTokenRepository.save(rt);
        return rt.getToken();
    }
}

