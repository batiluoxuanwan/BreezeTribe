package org.whu.backend.repository.travelRepo;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.whu.backend.entity.accounts.Merchant;
import org.whu.backend.entity.travelpac.TravelPackage;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TravelPackageRepository extends JpaRepository<TravelPackage, String> {
    // 根据状态查询旅行团（分页）
    Page<TravelPackage> findByStatus(TravelPackage.PackageStatus status, Pageable pageable);

    // 不分页
    List<TravelPackage> findByStatus(TravelPackage.PackageStatus status);

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
    @Query("UPDATE TravelPackage p SET p.commentCount = p.commentCount - :count WHERE p.id = :packageId AND p.commentCount >= :count")
    void decrementCommentCount(String packageId, long count);

    // 原子化地增加浏览量
    @Modifying
    @Query("UPDATE TravelPackage p SET p.viewCount = p.viewCount + 1 WHERE p.id = :packageId")
    void incrementViewCount(String packageId);

    // 原子化地增加报名人数数
    @Modifying
    @Query("UPDATE TravelPackage p SET p.participants = p.participants + :num WHERE p.id = :packageId")
    void addParticipantCount(String packageId, Integer num);

    // 原子化地减少报名人数数
    @Modifying
    @Query("UPDATE TravelPackage p SET p.participants = GREATEST(p.participants - :num, 0) WHERE p.id = :packageId")
    void subParticipantCount(String packageId, Integer num);

    // 查询某个团是否属于某个经销商
    boolean existsByIdAndDealerId(String packageId, String currentDealerId);

    boolean existsByIdAndStatus(String packageId, TravelPackage.PackageStatus packageStatus);

    Collection<TravelPackage> findTop10ByOrderByCreatedTimeDesc();

    List<TravelPackage> findByStatusAndIdNotIn(TravelPackage.PackageStatus status, Collection<String> ids);

    List<TravelPackage> findByDealerAndStatus(Merchant merchant, TravelPackage.PackageStatus packageStatus);

    @Query("""
        SELECT COALESCE(SUM(tp.viewCount), 0)
        FROM TravelPackage tp
        WHERE tp.dealer.id = :merchantId
          AND tp.createdTime >= :start
          AND tp.createdTime < :end
    """)
    Long sumViewCountByMerchantAndPeriod(@Param("merchantId") String merchantId,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    @Query("""
        SELECT COALESCE(SUM(tp.favoriteCount), 0)
        FROM TravelPackage tp
        WHERE tp.dealer.id = :merchantId
          AND tp.createdTime >= :start
          AND tp.createdTime < :end
    """)
    Long sumFavoriteCountByMerchantAndPeriod(@Param("merchantId") String merchantId,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    @Query("""
        SELECT COALESCE(SUM(tp.commentCount), 0)
        FROM TravelPackage tp
        WHERE tp.dealer.id = :merchantId
          AND tp.createdTime >= :start
          AND tp.createdTime < :end
    """)
    Long sumCommentCountByMerchantAndPeriod(@Param("merchantId") String merchantId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    @Query("""
        SELECT COALESCE(SUM(tp.viewCount), 0)
        FROM TravelPackage tp
        WHERE tp.id = :packageId
          AND tp.createdTime >= :start
          AND tp.createdTime < :end
    """)
    Long sumViewCountByPackageAndPeriod(@Param("packageId") String packageId,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    @Query("""
        SELECT COALESCE(SUM(tp.favoriteCount), 0)
        FROM TravelPackage tp
        WHERE tp.id = :packageId
          AND tp.createdTime >= :start
          AND tp.createdTime < :end
    """)
    Long sumFavoriteCountByPackageAndPeriod(@Param("packageId") String packageId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    @Query("""
        SELECT COALESCE(SUM(tp.commentCount), 0)
        FROM TravelPackage tp
        WHERE tp.id = :packageId
          AND tp.createdTime >= :start
          AND tp.createdTime < :end
    """)
    Long sumCommentCountByPackageAndPeriod(@Param("packageId") String packageId,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    List<TravelPackage> findAllByStatus(TravelPackage.PackageStatus packageStatus);

    List<TravelPackage> findAllByStatusOrderByHiddenScoreDesc(TravelPackage.PackageStatus packageStatus);
}
