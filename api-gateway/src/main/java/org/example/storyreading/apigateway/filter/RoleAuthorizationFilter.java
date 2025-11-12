package org.example.storyreading.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class RoleAuthorizationFilter implements GlobalFilter, Ordered {

    private static final List<String> ADMIN_ENDPOINTS = List.of(
            "/api/admin"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Kiểm tra nếu là admin endpoint
        if (isAdminEndpoint(path)) {
            String userRole = request.getHeaders().getFirst("X-User-Role");
            
            if (userRole == null || !userRole.equals("ADMIN")) {
                return onError(exchange, "Access denied. Admin role required.", HttpStatus.FORBIDDEN);
            }
        }

        return chain.filter(exchange);
    }

    private boolean isAdminEndpoint(String path) {
        return ADMIN_ENDPOINTS.stream().anyMatch(path::startsWith);
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
        return -50; // Chạy sau JwtAuthenticationFilter
    }
}

