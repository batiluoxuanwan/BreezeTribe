package org.whu.backend.repository.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.whu.backend.entity.travelpost.Comment;

import java.util.List;
import java.util.Set;

@Repository
public interface PostCommentRepository extends JpaRepository<Comment, String> {
    // 分页查询指定游记下的一级评论 (parent_id is null)
    Page<Comment> findByTravelPostIdAndParentIsNull(String postId, Pageable pageable);

    // 查询指定父评论下的前N条回复，用于预览
    List<Comment> findTop3ByParentIdOrderByCreatedTimeAsc(String parentId);

    // 统计指定父评论下的回复总数
    long countByParentId(String parentId);



    // 使用原生SQL的递归查询，分页获取一个评论下的所有子孙回复
    @Query(value = "WITH RECURSIVE ReplyTree AS (" +
            "  SELECT * FROM post_comments WHERE parent_id = :commentId" + // 1. 锚点成员：找到直接回复
            "  UNION ALL" +
            "  SELECT c.* FROM post_comments c JOIN ReplyTree rt ON c.parent_id = rt.id" + // 2. 递归成员：不断向下查找
            ") SELECT * FROM ReplyTree",
            countQuery = "WITH RECURSIVE ReplyTree AS (" +
                    "  SELECT * FROM post_comments WHERE parent_id = :commentId" +
                    "  UNION ALL" +
                    "  SELECT c.* FROM post_comments c JOIN ReplyTree rt ON c.parent_id = rt.id" +
                    ") SELECT count(*) FROM ReplyTree",
            nativeQuery = true)
    Page<Comment> findAllRepliesByParentId(String commentId, Pageable pageable);

    /**
     *  根据游记ID批量删除所有评论
     */
    @Modifying
    void deleteAllByTravelPostId(String postId);

    /**
     *  检查一条评论是否有任何直接的子回复
     */
    boolean existsByParentId(String parentId);

    /**
     * 使用原生SQL递归查询，一次性找出一条评论下的所有后代评论的ID
     * 这用于“铲掉整栋楼”的操作
     */
    @Query(value = "WITH RECURSIVE ReplyTree AS (" +
            "  SELECT id FROM post_comments WHERE parent_id = :commentId" +
            "  UNION ALL" +
            "  SELECT c.id FROM post_comments c JOIN ReplyTree rt ON c.parent_id = rt.id" +
            ") SELECT id FROM ReplyTree",
            nativeQuery = true)
    List<String> findAllDescendantIds(String commentId);

    /**
     * 批量删除（逻辑删除）一组评论
     * Spring Data JPA 默认的 deleteAllByIdInBatch 是物理删除，我们需要用@Query自定义
     */
//    @Modifying // TODO：如果软删除无效再改
//    @Query("UPDATE Comment c SET c.deleted = true WHERE c.id IN :ids")
    void softDeleteAllByIds(Set<String> ids);
}
