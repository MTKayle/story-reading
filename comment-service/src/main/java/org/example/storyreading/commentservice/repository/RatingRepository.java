package org.example.storyreading.commentservice.repository;

import jakarta.transaction.Transactional;
import org.example.storyreading.commentservice.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByUserIdAndStoryId(Long userId, Long storyId);

    @Query("SELECT AVG(r.stars) FROM Rating r WHERE r.storyId = :storyId")
    Double getAverageRating(@Param("storyId") Long storyId);

    void deleteByUserIdAndStoryId(Long userId, Long storyId);
    // ✅ Xóa tất cả rating thuộc về storyId
    @Transactional
    void deleteByStoryId(Long storyId);
    // ✅ Lấy danh sách id của rating theo storyId
    @Query("SELECT r.id FROM Rating r WHERE r.storyId = :storyId")
    List<Long> findIdsByStoryId(@Param("storyId") Long storyId);
}
