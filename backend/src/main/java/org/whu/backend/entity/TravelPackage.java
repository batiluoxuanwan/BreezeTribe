package org.whu.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@SoftDelete
@Table(name = "travel_packages")
public class TravelPackage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String title; // 旅行团标题, e.g., "法意瑞10日豪华团"

    private String coverImageUrl; // 封面图URL (存放在阿里云OSS)

    @Column(columnDefinition = "TEXT")
    private String detailedDescription; // 图文详情

    @Column(nullable = false)
    private BigDecimal price; // 价格

    private LocalDateTime departureDate; // 出发日期

    private Integer durationInDays; // 行程天数

    @Enumerated(EnumType.STRING)
    private PackageStatus status; // 状态: DRAFT(草稿), PENDING_APPROVAL(待审核), PUBLISHED(已发布), REJECTED(已驳回)

    // 一个旅行团包含多条路线 (比如 Day1, Day2 的路线)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "package_routes",
            joinColumns = @JoinColumn(name = "package_id"),
            inverseJoinColumns = @JoinColumn(name = "route_id"))
    @OrderBy("day_number ASC") // 假设中间表有个day_number来表示第几天
    private List<Route> routes = new ArrayList<>();

    // TODO: 等待用户模块完成后，这里应改为 @ManyToOne private DealerAccount dealer;
    @Column(nullable = false)
    private String dealerAccountId;

    @Column(nullable = false, insertable = false, updatable = false)
    private boolean deleted = false; // 删除标记

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedTime;

    public enum PackageStatus {
        DRAFT, PENDING_APPROVAL, PUBLISHED, REJECTED
    }
}