package org.whu.backend.repository.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.whu.backend.entity.travelpost.TravelPost;

import java.util.Collection;
import java.util.List;

@Repository
public interface TravelPostRepository extends JpaRepository<TravelPost, String> {
    // 根据作者ID分页查询游记
    Page<TravelPost> findByAuthorId(String authorId, Pageable pageable);

    // 原子化地增加收藏数
    @Modifying
    @Query("UPDATE TravelPost p SET p.favoriteCount = p.favoriteCount + 1 WHERE p.id = :postId")
    void incrementFavoriteCount(String postId);

    // 原子化地减少收藏数
    @Modifying
    @Query("UPDATE TravelPost p SET p.favoriteCount = p.favoriteCount - 1 WHERE p.id = :postId AND p.favoriteCount > 0")
    void decrementFavoriteCount(String postId);

    // 原子化地增加评论数
    @Modifying
    @Query("UPDATE TravelPost p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    void incrementCommentCount(String postId);

    // 原子化地减少评论数
    @Modifying
    @Query("UPDATE TravelPost p SET p.commentCount = p.commentCount - 1 WHERE p.id = :postId AND p.commentCount > 0")
    void decrementCommentCount(String postId);

    @Modifying
    @Query("UPDATE TravelPost p SET p.commentCount = GREATEST(p.commentCount - :num, 0 )WHERE p.id = :postId AND p.commentCount > 0")
    void decrementCommentCount(String postId, long num);

    // 原子化地增加点赞数
    @Modifying
    @Query("UPDATE TravelPost p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId")
    void incrementLikeCount(String postId);

    // 原子化地减少收藏数
    @Modifying
    @Query("UPDATE TravelPost p SET p.likeCount = p.likeCount - 1 WHERE p.id = :postId AND p.likeCount > 0")
    void decrementLikeCount(String postId);

    // 原子化地增加浏览量
    @Modifying
    @Query("UPDATE TravelPost p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    void incrementViewCount(String postId);

    // 复杂的搜索查询
    Page<TravelPost> findAll(Specification<TravelPost> spec, Pageable pageable);

    Collection<TravelPost> findTop10ByOrderByCreatedTimeDesc();

    List<TravelPost> findTop100ByOrderByViewCountDesc();
}