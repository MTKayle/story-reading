package org.example.storyreading.userservice.security;

import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;

import java.security.Key;
import java.util.Date;


//generate and extract token
@Component
public class JwtUtils {
    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.expiration}")
    private long expirationTime;

    private Key getKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    // Generate a JWT token
    public String generateToken(Long userId, String username, String role, String email) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("role", role)
                .claim("email", email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Extract username from the JWT token
    public String extractUsername(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    public String extractRole(String token) {
        return (String) getAllClaimsFromToken(token).get("role");
    }

    public String extractEmail(String token) {
        return (String) getAllClaimsFromToken(token).get("email");
    }

    public Long extractUserId(String token) {
        Object val = getAllClaimsFromToken(token).get("userId");
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        if (val instanceof String) {
            try { return Long.parseLong((String) val); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.out.println("Token đã hết hạn!");
        } catch (UnsupportedJwtException e) {
            System.out.println("Token không hỗ trợ!");
        } catch (MalformedJwtException e) {
            System.out.println("Token không hợp lệ!");
        } catch (SignatureException e) {
            System.out.println("Chữ ký token không đúng!");
        } catch (IllegalArgumentException e) {
            System.out.println("Token trống hoặc null!");
        }
        return false;
    }
}