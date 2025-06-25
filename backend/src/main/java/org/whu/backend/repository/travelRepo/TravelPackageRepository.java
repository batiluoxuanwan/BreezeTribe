package org.whu.backend.repository.travelRepo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.whu.backend.entity.TravelPackage;

import java.util.Optional;

public interface TravelPackageRepository extends JpaRepository<TravelPackage, String> {
    /**
     * 根据状态查询旅行团（分页）
     * Spring Data JPA会根据方法名自动生成查询语句。
     * @param status 状态
     * @param pageable 分页信息
     * @return 分页后的旅行团
     */
    Page<TravelPackage> findByStatus(TravelPackage.PackageStatus status, Pageable pageable);

    /**
     * 根据ID和状态查询单个旅行团
     * @param id 旅行团ID
     * @param status 状态
     * @return 查询到的旅行团（Optional）
     */
    Optional<TravelPackage> findByIdAndStatus(String id, TravelPackage.PackageStatus status);
}
