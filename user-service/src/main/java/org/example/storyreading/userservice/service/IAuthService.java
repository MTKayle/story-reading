package org.example.storyreading.userservice.service;

import org.example.storyreading.userservice.dto.AuthDtos;
import org.example.storyreading.userservice.dto.GoogleLoginRequest;

public interface IAuthService {
    AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request);
    AuthDtos.AuthResponse login(AuthDtos.LoginRequest request);
    AuthDtos.AuthResponse refresh(AuthDtos.RefreshTokenRequest request);
    AuthDtos.AuthResponse loginWithGoogle(GoogleLoginRequest request);
}
