package org.example.storyreading.commentservice.repository;

import org.example.storyreading.commentservice.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // chỉ lấy các bình luận chưa bị xóa hoặc chặn
    List<Comment> findByChapterIdAndIsDeletedOrderByCreatedAtAsc(Long chapterId, String isDeleted);
    List<Comment> findByChapterIdAndStoryIdAndIsDeletedOrderByCreatedAtAsc(Long chapterId, Long storyId, String isDeleted);
    // Lấy userId của chủ bình luận theo commentId
    @Query("SELECT c.userId FROM Comment c WHERE c.id = :commentId")
    Long findUserIdByCommentId(Long commentId);
    // ✅ Lấy tất cả bình luận theo storyId (chưa bị xóa mềm)
    List<Comment> findByStoryIdAndIsDeletedOrderByCreatedAtAsc(Long storyId, String isDeleted);
    // Lấy danh sách comment theo parentId
    List<Comment> findByParentId(Long parentId);
    // Lấy các comment con theo parentId và chưa xóa
    List<Comment> findByParentIdAndIsDeleted(Long parentId, String isDeleted);
    // Nếu chỉ cần commentId
    List<Long> findIdByParentId(Long parentId);
    // Lấy tất cả comment chưa xóa theo storyId và chapterId = null
    List<Comment> findByStoryIdAndChapterIdIsNullAndIsDeletedOrderByCreatedAtAsc(Long storyId, String isDeleted);
}



