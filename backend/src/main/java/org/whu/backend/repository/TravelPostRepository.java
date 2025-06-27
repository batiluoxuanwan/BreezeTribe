package org.whu.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.whu.backend.entity.TravelPackage;
import org.whu.backend.entity.travelpost.TravelPost;

@Repository
public interface TravelPostRepository extends
        JpaRepository<TravelPost, String>,
        JpaSpecificationExecutor<TravelPackage> {
    // 根据作者ID分页查询游记
    Page<TravelPost> findByAuthorId(String authorId, Pageable pageable);

}