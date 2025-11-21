package org.example.storyreading.storyservice.service.impl;

import org.example.storyreading.storyservice.dto.GenreDtos;
import org.example.storyreading.storyservice.entity.GenreEntity;
import org.example.storyreading.storyservice.entity.StoryEntity;
import org.example.storyreading.storyservice.repository.GenreRepository;
import org.example.storyreading.storyservice.repository.StoryRepository;
import org.example.storyreading.storyservice.service.IGenreService;
import org.example.storyreading.storyservice.util.SlugUtil;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GenreService implements IGenreService {

    private final GenreRepository genreRepository;
    private final StoryRepository storyRepository;

    public GenreService(GenreRepository genreRepository, StoryRepository storyRepository) {
        this.genreRepository = genreRepository;
        this.storyRepository = storyRepository;
    }

    @Override
    public List<GenreDtos.GenreResponse> getAll() {
        return genreRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public GenreDtos.GenreResponse create(GenreDtos.GenreRequest request) {
        String name = sanitizeName(request.name);
        if (genreRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Thể loại đã tồn tại");
        }
        String slug = ensureUniqueSlug(SlugUtil.slugify(name), null);

        GenreEntity entity = new GenreEntity();
        entity.setName(name);
        entity.setSlug(slug);
        entity.setDescription(normalizeDescription(request.description));
        entity = genreRepository.save(entity);

        return toDto(entity);
    }

    @Override
    @Transactional
    public GenreDtos.GenreResponse update(Long id, GenreDtos.GenreRequest request) {
        GenreEntity entity = genreRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thể loại"));

        String oldName = entity.getName();
        String newName = sanitizeName(request.name);
        if (!oldName.equalsIgnoreCase(newName) && genreRepository.existsByNameIgnoreCase(newName)) {
            throw new IllegalArgumentException("Thể loại đã tồn tại");
        }

        if (!oldName.equalsIgnoreCase(newName)) {
            entity.setName(newName);
            entity.setSlug(ensureUniqueSlug(SlugUtil.slugify(newName), entity.getId()));
            replaceGenreNameInStories(oldName, newName);
        }

        entity.setDescription(normalizeDescription(request.description));
        entity = genreRepository.save(entity);
        return toDto(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        GenreEntity entity = genreRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thể loại"));

        removeGenreFromStories(entity.getName());
        genreRepository.delete(entity);
    }

    private GenreDtos.GenreResponse toDto(GenreEntity entity) {
        GenreDtos.GenreResponse dto = new GenreDtos.GenreResponse();
        dto.id = entity.getId();
        dto.name = entity.getName();
        dto.slug = entity.getSlug();
        dto.description = entity.getDescription();
        dto.storyCount = storyRepository.countByGenresContainingIgnoreCase(entity.getName());
        return dto;
    }

    private String sanitizeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Tên thể loại không được để trống");
        }
        return name.trim();
    }

    private String normalizeDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        return description.trim();
    }

    private String ensureUniqueSlug(String baseSlug, Long currentId) {
        if (!StringUtils.hasText(baseSlug)) {
            baseSlug = "genre";
        }
        String candidate = baseSlug;
        int counter = 1;
        while (true) {
            var existing = genreRepository.findBySlug(candidate);
            if (existing.isEmpty()) {
                return candidate;
            }
            if (currentId != null && existing.get().getId().equals(currentId)) {
                return candidate;
            }
            candidate = baseSlug + "-" + counter++;
        }
    }

    private void replaceGenreNameInStories(String oldName, String newName) {
        List<StoryEntity> stories = storyRepository.findByGenresContainingIgnoreCase(oldName);
        for (StoryEntity story : stories) {
            boolean changed = false;
            List<String> parsed = parseGenres(story.getGenres());
            List<String> updated = new ArrayList<>();
            for (String g : parsed) {
                if (g.equalsIgnoreCase(oldName)) {
                    updated.add(newName);
                    changed = true;
                } else {
                    updated.add(g);
                }
            }
            if (changed) {
                story.setGenres(joinGenres(updated));
                storyRepository.save(story);
            }
        }
    }

    private void removeGenreFromStories(String name) {
        List<StoryEntity> stories = storyRepository.findByGenresContainingIgnoreCase(name);
        for (StoryEntity story : stories) {
            List<String> parsed = parseGenres(story.getGenres());
            List<String> updated = parsed.stream()
                    .filter(g -> !g.equalsIgnoreCase(name))
                    .collect(Collectors.toList());
            if (updated.size() != parsed.size()) {
                story.setGenres(joinGenres(updated));
                storyRepository.save(story);
            }
        }
    }

    private List<String> parseGenres(String genres) {
        if (!StringUtils.hasText(genres)) {
            return new ArrayList<>();
        }
        return Arrays.stream(genres.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private String joinGenres(List<String> genres) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        for (String genre : genres) {
            ordered.add(genre.trim());
        }
        return String.join(",", ordered);
    }
}

