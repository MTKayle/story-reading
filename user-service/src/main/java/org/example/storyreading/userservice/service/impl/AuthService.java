package org.example.storyreading.userservice.service.impl;

import org.example.storyreading.userservice.dto.AuthDtos;
import org.example.storyreading.userservice.entity.RefreshTokenEntity;
import org.example.storyreading.userservice.entity.RoleEntity;
import org.example.storyreading.userservice.entity.UserEntity;
import org.example.storyreading.userservice.entity.UserStatus;
import org.example.storyreading.userservice.repository.RefreshTokenRepository;
import org.example.storyreading.userservice.repository.RoleRepository;
import org.example.storyreading.userservice.repository.UserRepository;
import org.example.storyreading.userservice.security.JwtUtils;
import org.example.storyreading.userservice.service.IAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
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
        // Username là bắt buộc, lấy từ request.username hoặc request.name
        String username = request.username;
        if (username == null || username.trim().isEmpty()) {
            if (request.name != null && !request.name.trim().isEmpty()) {
                username = request.name.trim();
            } else {
                throw new IllegalArgumentException("Username hoặc tên là bắt buộc");
            }
        }
        username = username.trim();

        // Kiểm tra username đã tồn tại
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }
        
        // Email là tùy chọn, nhưng nếu có thì phải unique
        String email = request.email;
        if (email != null && !email.trim().isEmpty()) {
            email = email.trim();
            // Kiểm tra email hợp lệ
            if (!email.contains("@")) {
                throw new IllegalArgumentException("Email không hợp lệ");
            }
            // Chặn đăng ký với email admin
            if ("thanhvanguyen90@gmail.com".equalsIgnoreCase(email)) {
                throw new IllegalArgumentException("Email này không thể được sử dụng để đăng ký");
            }
            if (userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("Email đã tồn tại");
            }
        } else {
            email = null; // Email không bắt buộc
        }

        // Luôn tạo user với role USER - không cho phép đăng ký admin
        RoleEntity role = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    RoleEntity r = new RoleEntity();
                    r.setName("USER");
                    return roleRepository.save(r);
                });

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email); // Có thể là null
        user.setPassword(passwordEncoder.encode(request.password));
        user.setRole(role);
        user = userRepository.save(user); // Lưu vào database

        // Tạo token với email có thể null
        String userEmail = user.getEmail() != null ? user.getEmail() : "";
        String accessToken = jwtUtils.generateToken(user.getId(), user.getUsername(), role.getName(), userEmail);
        String refreshToken = issueOrRotateRefreshToken(user);
        
        // Tạo UserDto để trả về
        org.example.storyreading.userservice.dto.UserDto userDto = new org.example.storyreading.userservice.dto.UserDto();
        userDto.id = user.getId();
        userDto.username = user.getUsername();
        userDto.email = user.getEmail(); // Có thể null
        userDto.avatarUrl = user.getAvatarUrl();
        userDto.bio = user.getBio();
        userDto.role = user.getRole().getName();
        userDto.status = user.getStatus() != null ? user.getStatus().name() : UserStatus.ACTIVE.name();
        userDto.createdAt = user.getCreatedAt();
        userDto.updatedAt = user.getUpdatedAt();
        userDto.lockedAt = user.getLockedAt();
        userDto.lockReason = user.getLockReason();
        
        return new AuthDtos.AuthResponse(accessToken, refreshToken, userDto);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        // Hỗ trợ cả usernameOrEmail và email
        String identifier = request.usernameOrEmail;
        if (identifier == null || identifier.trim().isEmpty()) {
            identifier = request.email;
        }
        
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập email hoặc username");
        }

        Optional<UserEntity> userOpt = userRepository.findByUsernameOrEmail(identifier, identifier);
        UserEntity user = userOpt.orElseThrow(() -> new IllegalArgumentException("Sai thông tin đăng nhập"));

        if (!passwordEncoder.matches(request.password, user.getPassword())) {
            throw new IllegalArgumentException("Sai thông tin đăng nhập");
        }

        String userEmail = user.getEmail() != null ? user.getEmail() : "";
        String accessToken = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole().getName(), userEmail);
        String refreshToken = ensureRefreshToken(user);
        
        // Tạo UserDto để trả về
        org.example.storyreading.userservice.dto.UserDto userDto = new org.example.storyreading.userservice.dto.UserDto();
        userDto.id = user.getId();
        userDto.username = user.getUsername();
        userDto.email = user.getEmail();
        userDto.avatarUrl = user.getAvatarUrl();
        userDto.bio = user.getBio();
        userDto.role = user.getRole().getName();
        userDto.status = user.getStatus() != null ? user.getStatus().name() : UserStatus.ACTIVE.name();
        userDto.createdAt = user.getCreatedAt();
        userDto.updatedAt = user.getUpdatedAt();
        userDto.lockedAt = user.getLockedAt();
        userDto.lockReason = user.getLockReason();
        
        return new AuthDtos.AuthResponse(accessToken, refreshToken, userDto);
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
        String userEmail = user.getEmail() != null ? user.getEmail() : "";
        String accessToken = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole().getName(), userEmail);

        // Rotate refresh token để tăng bảo mật
        String newRefresh = rotateRefreshToken(user);
        
        // Tạo UserDto để trả về
        org.example.storyreading.userservice.dto.UserDto userDto = new org.example.storyreading.userservice.dto.UserDto();
        userDto.id = user.getId();
        userDto.username = user.getUsername();
        userDto.email = user.getEmail();
        userDto.avatarUrl = user.getAvatarUrl();
        userDto.bio = user.getBio();
        userDto.role = user.getRole().getName();
        userDto.status = user.getStatus() != null ? user.getStatus().name() : UserStatus.ACTIVE.name();
        userDto.createdAt = user.getCreatedAt();
        userDto.updatedAt = user.getUpdatedAt();
        userDto.lockedAt = user.getLockedAt();
        userDto.lockReason = user.getLockReason();
        
        return new AuthDtos.AuthResponse(accessToken, newRefresh, userDto);
    }

    @Override
    @Transactional
    public AuthDtos.AuthResponse googleAuth(AuthDtos.GoogleAuthRequest request) {
        if (request.idToken == null || request.idToken.trim().isEmpty()) {
            throw new IllegalArgumentException("ID Token không được để trống");
        }

        try {
            // Decode Google ID Token (JWT) để lấy thông tin
            String[] parts = request.idToken.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("ID Token không hợp lệ");
            }

            // Decode payload (phần thứ 2)
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> claims = mapper.readValue(payload, Map.class);

            // Lấy thông tin từ claims
            String email = (String) claims.get("email");
            String name = (String) claims.get("name");
            String givenName = (String) claims.get("given_name");
            String familyName = (String) claims.get("family_name");
            String picture = (String) claims.get("picture");

            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("Email không tìm thấy trong ID Token");
            }

            // Tạo username từ email hoặc name
            String username = email.split("@")[0]; // Lấy phần trước @ làm username
            if (name != null && !name.trim().isEmpty()) {
                // Ưu tiên dùng name nếu có
                username = name.trim().replaceAll("\\s+", "").toLowerCase();
            }

            // Tìm user theo email
            Optional<UserEntity> userOpt = userRepository.findByEmail(email);
            UserEntity user;

            if (userOpt.isPresent()) {
                // User đã tồn tại, sử dụng user đó
                user = userOpt.get();
            } else {
                // Tạo user mới
                // Kiểm tra username đã tồn tại chưa
                String finalUsername = username;
                int suffix = 1;
                while (userRepository.existsByUsername(finalUsername)) {
                    finalUsername = username + suffix;
                    suffix++;
                }

                RoleEntity role = roleRepository.findByName("USER")
                        .orElseGet(() -> {
                            RoleEntity r = new RoleEntity();
                            r.setName("USER");
                            return roleRepository.save(r);
                        });

                user = new UserEntity();
                user.setUsername(finalUsername);
                user.setEmail(email);
                user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // Random password vì không cần
                user.setRole(role);
                user.setStatus(UserStatus.ACTIVE);
                user = userRepository.save(user);
            }

            // Tạo token
            String userEmail = user.getEmail() != null ? user.getEmail() : "";
            String accessToken = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole().getName(), userEmail);
            String refreshToken = ensureRefreshToken(user);

            // Tạo UserDto để trả về
            org.example.storyreading.userservice.dto.UserDto userDto = new org.example.storyreading.userservice.dto.UserDto();
            userDto.id = user.getId();
            userDto.username = user.getUsername();
            userDto.email = user.getEmail();
            userDto.avatarUrl = user.getAvatarUrl();
            userDto.bio = user.getBio();
            userDto.role = user.getRole().getName();
            userDto.status = user.getStatus() != null ? user.getStatus().name() : UserStatus.ACTIVE.name();
            userDto.createdAt = user.getCreatedAt();
            userDto.updatedAt = user.getUpdatedAt();
            userDto.lockedAt = user.getLockedAt();
            userDto.lockReason = user.getLockReason();

            return new AuthDtos.AuthResponse(accessToken, refreshToken, userDto);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Lỗi xử lý Google ID Token: " + e.getMessage());
        }
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

