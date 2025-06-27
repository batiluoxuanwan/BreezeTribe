package org.whu.backend.repository.travelRepo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
