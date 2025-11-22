package org.example.storyreading.userservice.service;

import org.example.storyreading.userservice.dto.AuthDtos;

public interface IAuthService {
    AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request);
    AuthDtos.AuthResponse login(AuthDtos.LoginRequest request);
    AuthDtos.AuthResponse refresh(AuthDtos.RefreshTokenRequest request);
    AuthDtos.AuthResponse googleAuth(AuthDtos.GoogleAuthRequest request);
}

