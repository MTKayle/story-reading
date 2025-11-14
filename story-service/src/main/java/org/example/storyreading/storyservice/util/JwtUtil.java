package org.example.storyreading.storyservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    @Value("${jwt.secret:mySecretKey}")
    private String secretKey;

    /**
     * Giải mã JWT token và lấy userId từ claims
     * @param token JWT token (không có "Bearer " prefix)
     * @return userId hoặc null nếu token không hợp lệ
     */
    public Long extractUserId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey.getBytes())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Thử lấy userId từ các claim khác nhau
            Object userIdClaim = claims.get("userId");
            if (userIdClaim != null) {
                return Long.valueOf(userIdClaim.toString());
            }

            // Fallback: thử lấy từ subject
            String subject = claims.getSubject();
            if (subject != null) {
                try {
                    return Long.valueOf(subject);
                } catch (NumberFormatException e) {
                    // Subject không phải là số, bỏ qua
                }
            }

            // Thử claim "id"
            Object idClaim = claims.get("id");
            if (idClaim != null) {
                return Long.valueOf(idClaim.toString());
            }

            return null;
        } catch (Exception e) {
            // Token không hợp lệ hoặc hết hạn
            return null;
        }
    }

    /**
     * Lấy userId từ Authorization header
     * @param authorizationHeader Authorization header (có thể có "Bearer " prefix)
     * @return userId hoặc null
     */
    public Long extractUserIdFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
            return null;
        }

        // Loại bỏ "Bearer " prefix nếu có
        String token = authorizationHeader;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return extractUserId(token);
    }
}

