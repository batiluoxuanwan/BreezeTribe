package org.whu.backend.entity.travelpac;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 实体类 2: TravelDeparture (出发团期) - 【全新】
 * 职责：描述一个具体可售卖的团期实例，包含价格、日期、库存等。
 */
@Data
@Entity
@SoftDelete
@Table(name = "travel_departures") // 这是全新的表
public class TravelDeparture {
    @Id
    @Column(length = 36)
    private String id;

    // --- 团期特有信息 ---
    @Column(nullable = false)
    private LocalDateTime departureDate; // 具体的出发日期

    @Column(nullable = false)
    private java.math.BigDecimal price; // 这个团期的价格

    @Column(nullable = false)
    private Integer capacity; // 总容量/库存

    @ColumnDefault("0")
    private Integer participants; // 已报名人数

    @Enumerated(EnumType.STRING)
    private DepartureStatus status; // 团期状态，例如 OPEN (可报名), CLOSED (已报满), FINISHED (已结束)

    public enum DepartureStatus {OPEN, CLOSED, FINISHED}

    // --- 关联关系 ---
    // 多个团期实例，从属于同一个产品模板
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "package_id", nullable = false) // 关联回 travel_packages 表
    private TravelPackage travelPackage;

    // 一个团期可以有多个订单
    @OneToMany(mappedBy = "travelDeparture", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TravelOrder> travelOrders = new ArrayList<>();

    // --- 时间戳 ---
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdTime;

    @UpdateTimestamp
    private LocalDateTime updatedTime;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }
}