package org.example.storyreading.userservice.dto;

public class AuthDtos {

    public static class RegisterRequest {
        public String username;
        public String name; // Tên hiển thị, nếu không có username thì dùng name làm username
        public String email;
        public String password;
    }

    public static class LoginRequest {
        public String usernameOrEmail;
        public String email; // Hỗ trợ email trực tiếp
        public String password;
    }

    public static class RefreshTokenRequest {
        public String refreshToken;
    }

    public static class GoogleAuthRequest {
        public String idToken;
    }

    public static class AuthResponse {
        public String token; // accessToken, đổi tên để tương thích với frontend
        public String accessToken; // Giữ lại để tương thích
        public String refreshToken;
        public String tokenType = "Bearer";
        public UserDto user; // Thông tin user

        public AuthResponse(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.token = accessToken; // Đặt token = accessToken
            this.refreshToken = refreshToken;
        }

        public AuthResponse(String accessToken, String refreshToken, UserDto user) {
            this.accessToken = accessToken;
            this.token = accessToken;
            this.refreshToken = refreshToken;
            this.user = user;
        }
    }
}


