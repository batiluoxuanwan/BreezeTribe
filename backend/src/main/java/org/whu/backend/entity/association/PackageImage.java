package org.whu.backend.entity.association;

import jakarta.persistence.*;
import lombok.Data;
import org.whu.backend.entity.MediaFile;
import org.whu.backend.entity.travelpac.TravelPackage;

/**
 * 旅行团图片关联实体 (中间表)
 * 这张表是连接 TravelPackage 和 MediaFile 的桥梁。
 * 它的核心作用是记录“哪个旅行团使用了哪张图片，并且排在第几位”。
 */
@Data
@Entity
@Table(name = "package_images")
public class PackageImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 对于简单的关联表，用自增ID即可

    // 关联到哪个旅行团。
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "package_id", nullable = false)
    private TravelPackage travelPackage;

    //关联到哪张图片。
    @ManyToOne(fetch = FetchType.EAGER) // 通常我们希望加载关联信息时能直接看到图片URL
    @JoinColumn(name = "media_file_id", nullable = false)
    private MediaFile mediaFile;

    // 这张图片在这个旅行团的图册中排在第几位。
    @Column(nullable = false)
    private int sortOrder;
}