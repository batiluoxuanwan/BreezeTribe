package org.whu.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.whu.backend.entity.accounts.Merchant;
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

    // TODO: 注释掉，有关联表了
    private String coverImageUrl; // 封面的ObjectKey

    @Lob
    private String detailedDescription; // 旅行团描述

    @Column(nullable = false)
    private BigDecimal price; // 价格

    private Integer capacity; // 总容量

    private Integer participants; // 参与人数

    private LocalDateTime departureDate; // 出发日期

    private Integer durationInDays; // 持续天数

    @Enumerated(EnumType.STRING)
    private PackageStatus status; // 旅行团状态

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedTime;

    public enum PackageStatus {DRAFT, PENDING_APPROVAL, PUBLISHED, REJECTED}

    // 关联对应的经销商
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dealer_account_id", referencedColumnName = "id", nullable = false)
    private Merchant dealer;

    // 旅行团的图册。这是一个一对多的关系，指向关联实体 PackageImage。
    @OneToMany(mappedBy = "travelPackage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<PackageImage> images = new ArrayList<>();

    // 使用关系表PackageRoute关联旅游团和路线
    @OneToMany(mappedBy = "travelPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC") // 正确地按照关联实体中的字段排序
    private List<PackageRoute> routes = new ArrayList<>();

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