package org.example.storyreading.storyservice.repository;

import org.example.storyreading.storyservice.entity.GenreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GenreRepository extends JpaRepository<GenreEntity, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsBySlug(String slug);
    Optional<GenreEntity> findByNameIgnoreCase(String name);
    Optional<GenreEntity> findBySlug(String slug);
}

