package org.example.storyreading.storyservice.service;

import org.example.storyreading.storyservice.dto.GenreDtos;

import java.util.List;

public interface IGenreService {
    List<GenreDtos.GenreResponse> getAll();
    GenreDtos.GenreResponse create(GenreDtos.GenreRequest request);
    GenreDtos.GenreResponse update(Long id, GenreDtos.GenreRequest request);
    void delete(Long id);
}

