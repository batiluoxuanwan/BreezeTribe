package org.whu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.whu.backend.entity.association.PackageImage;

import java.util.Optional;

@Repository
public interface PackageImageRepository extends JpaRepository<PackageImage, Long> {
    // 检查一个MediaFile是否被任何PackageImage引用
    boolean existsByMediaFileId(String mediaFileId);

    Optional<PackageImage> findTopByMediaFileId(String fileId);
}