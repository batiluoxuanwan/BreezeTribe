package org.whu.backend.repository.travelRepo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.whu.backend.entity.PackageComment;

import java.util.List;

@Repository
public interface PackageCommentRepository extends JpaRepository<PackageComment, String> {
    // 分页查询指定旅行团下的一级评价
    Page<PackageComment> findByTravelPackageIdAndParentIsNull(String packageId, Pageable pageable);

    // 查询指定父评价下的前N条回复，用于预览
    List<PackageComment> findTop3ByParentIdOrderByCreatedTimeAsc(String parentId);

    // 统计指定父评价下的回复总数
    long countByParentId(String parentId);

    Page<PackageComment> findByParentId(String commentId, Pageable repliesPageable);
}
