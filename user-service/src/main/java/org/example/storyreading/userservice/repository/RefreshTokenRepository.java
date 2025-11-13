package org.example.storyreading.userservice.repository;

import org.example.storyreading.userservice.entity.RefreshTokenEntity;
import org.example.storyreading.userservice.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {
    Optional<RefreshTokenEntity> findByToken(String token);
    Optional<RefreshTokenEntity> findByUser(UserEntity user);
    void deleteByUser(UserEntity user);
}


