package org.whu.backend.repository.travelRepo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.whu.backend.entity.travelpac.PackageComment;
import org.whu.backend.entity.travelpac.TravelPackage;

import java.util.List;
import java.util.Set;

@Repository
public interface PackageCommentRepository extends JpaRepository<PackageComment, String> {
    // 分页查询指定旅行团下的一级评价
    Page<PackageComment> findByTravelPackageIdAndParentIsNull(String packageId, Pageable pageable);

    // 查询指定父评价下的前N条回复，用于预览
    List<PackageComment> findTop3ByParentIdOrderByCreatedTimeAsc(String parentId);

    // 统计指定父评价下的回复总数
    long countByParentId(String parentId);

    Page<PackageComment> findByParentId(String commentId, Pageable repliesPageable);
    List<PackageComment> findByParentId(String commentId);

    // 一次性查询出某个用户评价过的所有旅行团的ID
    @Query("SELECT pc.travelPackage.id FROM PackageComment pc WHERE pc.author.id = :userId")
    Set<String> findTravelPackageIdsReviewedByUser(String userId);

    // 根据父评论ID，批量删除所有直接回复
    @Modifying
    void deleteAllByParentId(String parentId);

    // 查询用户是否已经评价过一个旅游团
    boolean existsByAuthorIdAndTravelPackageId(String userId, String packageId);

    List<PackageComment> findByTravelPackageAndParentIsNull(TravelPackage travelPackage);

    /**
     * 根据作者ID和一组产品ID，一次性查询出所有相关的评论
     */
    List<PackageComment> findByAuthorIdAndTravelPackageIdIn(String authorId, List<String> packageIds);
}
