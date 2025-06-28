package org.whu.backend.repository.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.whu.backend.entity.travelpost.Comment;

import java.util.List;

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
}
