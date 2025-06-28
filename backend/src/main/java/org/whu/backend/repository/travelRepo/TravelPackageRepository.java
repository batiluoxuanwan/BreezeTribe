package org.whu.backend.repository.travelRepo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.whu.backend.entity.TravelPackage;

import java.util.Optional;

public interface TravelPackageRepository extends JpaRepository<TravelPackage, String> {
    // 根据状态查询旅行团（分页）
    Page<TravelPackage> findByStatus(TravelPackage.PackageStatus status, Pageable pageable);

    // 根据ID和状态查询单个旅行团
    Optional<TravelPackage> findByIdAndStatus(String id, TravelPackage.PackageStatus status);

    // 根据经销商ID分页查询旅行团（分页）
    Page<TravelPackage> findByDealerId(String dealerId, Pageable pageable);

    Page<TravelPackage> findAll(Specification<TravelPackage> spec, Pageable pageable);

    // 原子化地增加收藏数
    @Modifying
    @Query("UPDATE TravelPackage p SET p.favoriteCount = p.favoriteCount + 1 WHERE p.id = :packageId")
    void incrementFavoriteCount(String packageId);

    // 原子化地减少收藏数
    @Modifying
    @Query("UPDATE TravelPackage p SET p.favoriteCount = p.favoriteCount - 1 WHERE p.id = :packageId AND p.favoriteCount > 0")
    void decrementFavoriteCount(String packageId);

    // 原子化地增加评论数
    @Modifying
    @Query("UPDATE TravelPackage p SET p.commentCount = p.commentCount + 1 WHERE p.id = :packageId")
    void incrementCommentCount(String packageId);

    // 原子化地减少评论数
    @Modifying
    @Query("UPDATE TravelPackage p SET p.commentCount = p.commentCount - 1 WHERE p.id = :packageId AND p.commentCount > 0")
    void decrementCommentCount(String packageId);

    // 原子化地增加浏览量
    @Modifying
    @Query("UPDATE TravelPackage p SET p.viewCount = p.viewCount + 1 WHERE p.id = :packageId")
    void incrementViewCount(String packageId);

    // 原子化地增加报名人数数
    @Modifying
    @Query("UPDATE TravelPackage p SET p.participants = p.participants + :num WHERE p.id = :packageId")
    void addParticipantCount(String packageId, Integer num);


}
