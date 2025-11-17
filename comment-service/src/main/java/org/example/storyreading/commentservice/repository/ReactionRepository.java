package org.example.storyreading.commentservice.repository;

import org.example.storyreading.commentservice.entity.Reaction;
import org.example.storyreading.commentservice.entity.Reaction.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    Optional<Reaction> findByUserIdAndCommentId(Long userId, Long commentId);
    long countByCommentIdAndType(Long commentId, ReactionType type);
    void deleteByUserIdAndCommentId(Long userId, Long commentId);
    // ✅ lấy danh sách id của reaction theo commentId
    @Query("SELECT r.id FROM Reaction r WHERE r.commentId = :commentId")
    List<Long> findIdsByCommentIdIn(@Param("commentId") Long commentId);

    // ✅ Các method cho Report (sử dụng ReactionType.REPORT)
    Optional<Reaction> findByUserIdAndCommentIdAndType(Long userId, Long commentId, ReactionType type);
    List<Reaction> findByCommentIdAndType(Long commentId, ReactionType type);
    void deleteByUserIdAndCommentIdAndType(Long userId, Long commentId, ReactionType type);
}
