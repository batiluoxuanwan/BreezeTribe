package org.whu.backend.entity.travelpac;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.whu.backend.entity.MediaFile;
import org.whu.backend.entity.Tag;
import org.whu.backend.entity.accounts.Merchant;
import org.whu.backend.entity.association.PackageImage;
import org.whu.backend.entity.association.PackageRoute;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

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

    @Column(nullable = true)
    private BigDecimal price; // 价格

    private Integer capacity; // 总容量

    // TODO: 这个东西可能要被废弃！！！！下放到Departure类
    private Integer participants; // 参与人数

    // TODO: 这个东西可能要被废弃！！！！下放到Departure类
    private LocalDateTime departureDate; // 出发日期

    private Integer durationInDays; // 持续天数


    // ------- 统计数据，属于产品模板 -----------

    @ColumnDefault("0")
    private Integer favoriteCount = 0; // 收藏量

    @ColumnDefault("0")
    private Integer commentCount = 0; // 评论数量

    @ColumnDefault("0")
    private Integer viewCount = 0; // 浏览量

    // 可能作废了
    @ColumnDefault("0")
    private Integer salesCount = 0; // 销售量

    @Column(nullable = false)
    private Double averageRating = 0.0; // 大众评分

    @Column(nullable = false)
    private Double hiddenScore = 0.0; // 隐藏评分

    public void recalculateHiddenScore(double lambda, double k) {
        // Step 1: 计算原始热度得分（收藏、评论、浏览加权）
        double rawScore = 0.5 * favoriteCount + 0.3 * commentCount + 0.2 * viewCount;

        // Step 2: 归一化处理，防止 rawScore 无限增长
        double normalizedScore = rawScore / (rawScore + k); // 平滑增长曲线

        // Step 3: 时间衰减处理（越新分数越高）
        long days = ChronoUnit.DAYS.between(createdTime.toLocalDate(), LocalDate.now());
        double decay = Math.exp(-lambda * days); // 衰减因子

        // Step 4: 综合打分（新鲜度 + 热度）
        this.hiddenScore = (0.7 * decay + 0.3) * normalizedScore;
    }


    // 【新增关联】一个产品模板，可以有多个具体的出发团期
    @OneToMany(mappedBy = "travelPackage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TravelDeparture> departures = new ArrayList<>();


    @Enumerated(EnumType.STRING)
    private PackageStatus status; // 旅行团状态

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedTime;

//    // ---------标记是否已经同步到RAG知识库--------
//    @ColumnDefault("false")
//    private Boolean hasSyncedToRag = false; // 默认还没同步

    public enum PackageStatus {DRAFT, PENDING_APPROVAL, PUBLISHED, REJECTED}

    public String rejectionReason;//拒绝原因
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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "package_tags", // 这是JPA会自动创建的中间关联表
            joinColumns = @JoinColumn(name = "package_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

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