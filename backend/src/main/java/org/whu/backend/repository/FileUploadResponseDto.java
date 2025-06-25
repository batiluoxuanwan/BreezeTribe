package org.whu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.entity.MediaFile;

public interface FileUploadResponseDto extends JpaRepository<MediaFile, String> {
}
