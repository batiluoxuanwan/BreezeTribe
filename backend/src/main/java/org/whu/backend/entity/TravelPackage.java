package org.whu.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.whu.backend.entity.association.PackageImage;
import org.whu.backend.entity.association.PackageRoute;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 单个旅游团实体，一个旅游团实体可以有很多个路线
 */
@Data
@Entity
@SoftDelete
@Table(name = "travel_packages")
public class TravelPackage {
    @Id
    @Column(length = 36)
    private String id; // 36带横线UUID

    @Column(nullable = false)
    private String title; // 旅行团标题

    private String coverImageUrl; // 封面的ObjectKey

    @Lob
    private String detailedDescription; // 旅行团描述

    @Column(nullable = false)
    private BigDecimal price; // 价格

    private LocalDateTime departureDate; // 出发日期

    private Integer durationInDays; // 持续天数

    @Enumerated(EnumType.STRING)
    private PackageStatus status; // 旅行团状态

//    关联对应的经销商 TODO: 等Account实现
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "dealer_account_id", nullable = false)
//    private DealerAccount dealer;

    /**
     * 旅行团的图册。这是一个一对多的关系，指向我们的关联实体 PackageImage。
     * mappedBy = "travelPackage" 指明了在 PackageImage 实体中，是通过名为 'travelPackage' 的字段来维护关系的。
     * cascade = CascadeType.ALL 表示对旅行团的操作（如保存、删除）会级联到其关联的图片关系上。
     * orphanRemoval = true 表示如果从这个 images 列表中移除一个 PackageImage 对象，那么这个对象将从数据库中被删除。
     * OrderBy("sortOrder ASC") 会让JPA在查询时，自动按照 sortOrder 字段升序排列返回的图片列表。
     */
    @OneToMany(
            mappedBy = "travelPackage",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("sortOrder ASC")
    private List<PackageImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "travelPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC") // 正确地按照关联实体中的字段排序了！
    private List<PackageRoute> routes = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedTime;

    public enum PackageStatus { DRAFT, PENDING_APPROVAL, PUBLISHED, REJECTED }

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null || this.id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        }
    }

    // 辅助方法，方便地添加图片和排序
    public void addImage(MediaFile mediaFile, int sortOrder) {
        PackageImage packageImage = new PackageImage();
        packageImage.setTravelPackage(this);
        packageImage.setMediaFile(mediaFile);
        packageImage.setSortOrder(sortOrder);
        this.images.add(packageImage);
    }
}