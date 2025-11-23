package org.example.storyreading.apigateway.filter;

import org.example.storyreading.apigateway.security.JwtUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;

    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/google",
            "/public/"
    );
    
    // Allow GET requests to public static files
    private boolean isPublicStaticFile(String path, HttpMethod method) {
        return method == HttpMethod.GET && (path.startsWith("/public/") || path.startsWith("/public/images/"));
    }

    // Các endpoint public cho phép truy cập GET mà không cần xác thực
    private static  final List<String> PUBLIC_GET_ENDPOINTS = List.of(
            "/api/story", // Cho phép truy cập công khai đến các truyện công khai
            "/api/comments", // Cho phép đọc bình luận công khai
            "/api/user", // Cho phép đọc thông tin user công khai (để hiển thị tên/avatar trong comment)
            "/api/rating", // Cho phép đọc rating công khai
            "/api/reaction" // Cho phép đọc reaction công khai
    );

    //ham kiem tra public get endpoint
    private boolean isPublicGetEndpoint(String path) {
        return PUBLIC_GET_ENDPOINTS.stream().anyMatch(path::startsWith);
    }





    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Cho phép các endpoint public
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        HttpMethod method = request.getMethod();
        
        // Cho phép GET requests đến static files (avatars, images)
        if (isPublicStaticFile(path, method)) {
            return chain.filter(exchange);
        }

        // Các endpoint cần authentication ngay cả khi là GET
        if (path.contains("/purchase/check") || path.contains("/purchase")) {
            // Yêu cầu authentication cho purchase endpoints
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Authentication required", HttpStatus.UNAUTHORIZED);
            }
            
            String token = authHeader.substring(7);
            if (!jwtUtils.validateToken(token)) {
                return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }
            
            try {
                Long userId = jwtUtils.extractUserId(token);
                String role = jwtUtils.extractRole(token);
                String username = jwtUtils.extractUsername(token);

                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", userId != null ? userId.toString() : "")
                        .header("X-User-Role", role != null ? role : "")
                        .header("X-Username", username != null ? username : "")
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } catch (Exception e) {
                return onError(exchange, "Error processing token", HttpStatus.UNAUTHORIZED);
            }
        }

        // Kiểm tra token cho các endpoint khác
        String authHeader = request.getHeaders().getFirst("Authorization");
        
        // Xử lý POST comments - yêu cầu authentication
        if (path.startsWith("/api/comments") && method.name().equalsIgnoreCase("POST")) {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Authentication required to create comment", HttpStatus.UNAUTHORIZED);
            }
            
            String token = authHeader.substring(7);
            if (!jwtUtils.validateToken(token)) {
                return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }
            
            // Token hợp lệ, thêm user info và tiếp tục
            try {
                Long userId = jwtUtils.extractUserId(token);
                String role = jwtUtils.extractRole(token);
                String username = jwtUtils.extractUsername(token);

                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", userId != null ? userId.toString() : "")
                        .header("X-User-Role", role != null ? role : "")
                        .header("X-Username", username != null ? username : "")
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } catch (Exception e) {
                return onError(exchange, "Error processing token", HttpStatus.UNAUTHORIZED);
            }
        }

        // Xử lý POST rating - yêu cầu authentication
        if (path.startsWith("/api/rating") && method.name().equalsIgnoreCase("POST")) {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Authentication required to submit rating", HttpStatus.UNAUTHORIZED);
            }
            
            String token = authHeader.substring(7);
            if (!jwtUtils.validateToken(token)) {
                return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }
            
            // Token hợp lệ, thêm user info và tiếp tục
            try {
                Long userId = jwtUtils.extractUserId(token);
                String role = jwtUtils.extractRole(token);
                String username = jwtUtils.extractUsername(token);

                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", userId != null ? userId.toString() : "")
                        .header("X-User-Role", role != null ? role : "")
                        .header("X-Username", username != null ? username : "")
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } catch (Exception e) {
                return onError(exchange, "Error processing token", HttpStatus.UNAUTHORIZED);
            }
        }

        // Cho phép các GET request đến public GET endpoints mà không cần xác thực
        if (method.name().equalsIgnoreCase("GET") && isPublicGetEndpoint(path)) {
            return chain.filter(exchange);
        }
        
        // Các endpoint khác yêu cầu token bắt buộc
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);
        
        if (!jwtUtils.validateToken(token)) {
            return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
        }

        // Thêm thông tin user vào header để các service downstream có thể sử dụng
        try {
            Long userId = jwtUtils.extractUserId(token);
            String role = jwtUtils.extractRole(token);
            String username = jwtUtils.extractUsername(token);

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId != null ? userId.toString() : "")
                    .header("X-User-Role", role != null ? role : "")
                    .header("X-Username", username != null ? username : "")
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        } catch (Exception e) {
            return onError(exchange, "Error processing token", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        
        String errorJson = String.format("{\"error\":\"%s\"}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(errorJson.getBytes())));
    }

    @Override
    public int getOrder() {
        return -100; // Chạy sớm trong filter chain
    }
}

