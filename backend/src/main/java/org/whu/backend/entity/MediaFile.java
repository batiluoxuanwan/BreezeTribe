package org.whu.backend.entity;


import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 媒体文件实体 (中央图片/文件库)
 * 这是一个独立的、通用的文件实体，用于管理所有用户上传的图片、视频等。
 * 它只关心文件本身的信息，不关心它被用在了哪里。
 */
@Data
@Entity
@SoftDelete
@Table(name = "media_files")
public class MediaFile {
    @Id
    @Column(length = 36)
    private String id;

    // 存储在OSS上的唯一Key，用于文件管理和删除。
    @Column(nullable = false, unique = true)
    private String objectKey;

    // 文件的MIME类型，如 "image/jpeg", "image/png"。
    @Column(length = 50)
    private String fileType;

    // 文件大小（字节）。
    private Long fileSize;

    // 上传者的账户ID。用于权限校验和追踪。
    // TODO: 后续可以改为 @ManyToOne 与 Account 实体建立多对一关联。
    @Column(length = 36, nullable = false)
    private String uploaderId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedTime;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null || this.id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        }
    }
}