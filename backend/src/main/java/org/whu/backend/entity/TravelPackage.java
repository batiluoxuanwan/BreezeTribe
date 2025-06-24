package org.whu.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Column(length = 36, nullable = false)
    private String dealerAccountId; // TODO: 后续改为 @ManyToOne DealerAccount

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "package_routes",
            joinColumns = @JoinColumn(name = "package_id"),
            inverseJoinColumns = @JoinColumn(name = "route_id"))
    @OrderBy("day_number ASC")
    private List<Route> routes = new ArrayList<>(); // 多对多关系，Route和TravelPackage

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
}