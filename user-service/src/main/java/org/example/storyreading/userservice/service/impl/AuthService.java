package org.example.storyreading.userservice.service.impl;

import org.example.storyreading.userservice.dto.AuthDtos;
import org.example.storyreading.userservice.dto.GoogleLoginRequest;
import org.example.storyreading.userservice.dto.GoogleUserInfo;
import org.example.storyreading.userservice.entity.RefreshTokenEntity;
import org.example.storyreading.userservice.entity.RoleEntity;
import org.example.storyreading.userservice.entity.UserEntity;
import org.example.storyreading.userservice.repository.RefreshTokenRepository;
import org.example.storyreading.userservice.repository.RoleRepository;
import org.example.storyreading.userservice.repository.UserRepository;
import org.example.storyreading.userservice.security.JwtUtils;
import org.example.storyreading.userservice.service.GoogleOAuth2Service;
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
    private final GoogleOAuth2Service googleOAuth2Service;

    @Value("${refresh.expiration.minutes:43200}") // mặc định 30 ngày
    private long refreshExpirationMinutes;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils,
                       GoogleOAuth2Service googleOAuth2Service) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.googleOAuth2Service = googleOAuth2Service;
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

    @Override
    @Transactional
    public AuthDtos.AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        // 1. Verify Google ID Token
        GoogleUserInfo googleUserInfo = googleOAuth2Service.verifyGoogleToken(request.getIdToken());

        if (googleUserInfo == null) {
            throw new IllegalArgumentException("Google token không hợp lệ");
        }

        if (!googleUserInfo.isEmailVerified()) {
            throw new IllegalArgumentException("Email chưa được xác thực bởi Google");
        }

        // 2. Tìm user theo Google ID hoặc Email
        Optional<UserEntity> userOpt = userRepository.findByGoogleId(googleUserInfo.getGoogleId());

        UserEntity user;
        if (userOpt.isPresent()) {
            // User đã tồn tại với Google ID này
            user = userOpt.get();

            // Cập nhật thông tin mới nhất từ Google
            user.setAvatarUrl(googleUserInfo.getPicture());
            user = userRepository.save(user);
        } else {
            // Kiểm tra xem email đã được đăng ký chưa
            Optional<UserEntity> existingEmailUser = userRepository.findByEmail(googleUserInfo.getEmail());

            if (existingEmailUser.isPresent()) {
                // Email đã tồn tại nhưng chưa liên kết với Google
                user = existingEmailUser.get();
                user.setGoogleId(googleUserInfo.getGoogleId());
                user.setAvatarUrl(googleUserInfo.getPicture());
                user = userRepository.save(user);
            } else {
                // Tạo user mới từ Google account
                RoleEntity role = roleRepository.findByName("USER")
                        .orElseGet(() -> {
                            RoleEntity r = new RoleEntity();
                            r.setName("USER");
                            return roleRepository.save(r);
                        });

                user = new UserEntity();
                user.setGoogleId(googleUserInfo.getGoogleId());
                user.setEmail(googleUserInfo.getEmail());

                // Tạo username từ email hoặc name
                String baseUsername = googleUserInfo.getEmail().split("@")[0];
                String username = generateUniqueUsername(baseUsername);
                user.setUsername(username);

                // Set password random vì user đăng nhập bằng Google
                user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

                user.setAvatarUrl(googleUserInfo.getPicture());
                user.setRole(role);
                user = userRepository.save(user);
            }
        }

        // 3. Generate tokens
        String accessToken = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole().getName(), user.getEmail());
        String refreshToken = ensureRefreshToken(user);

        return new AuthDtos.AuthResponse(accessToken, refreshToken);
    }

    /**
     * Generate unique username từ base username
     */
    private String generateUniqueUsername(String baseUsername) {
        String username = baseUsername;
        int counter = 1;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
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
