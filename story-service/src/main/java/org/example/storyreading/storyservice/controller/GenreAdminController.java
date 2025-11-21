package org.example.storyreading.storyservice.controller;

import org.example.storyreading.storyservice.dto.GenreDtos;
import org.example.storyreading.storyservice.service.IGenreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/story/admin/genres")
public class GenreAdminController {

    private final IGenreService genreService;

    public GenreAdminController(IGenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping
    public ResponseEntity<List<GenreDtos.GenreResponse>> listGenres() {
        return ResponseEntity.ok(genreService.getAll());
    }

    @PostMapping
    public ResponseEntity<GenreDtos.GenreResponse> create(@RequestBody GenreDtos.GenreRequest request) {
        return ResponseEntity.ok(genreService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GenreDtos.GenreResponse> update(@PathVariable Long id,
                                                          @RequestBody GenreDtos.GenreRequest request) {
        return ResponseEntity.ok(genreService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        genreService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

