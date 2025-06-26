package org.whu.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.entity.MediaFile;

public interface MediaFileRepository extends JpaRepository<MediaFile, String> {
    //根据上传者ID分页查询媒体文件
    Page<MediaFile> findByUploaderId(String uploaderId, Pageable pageable);
}
