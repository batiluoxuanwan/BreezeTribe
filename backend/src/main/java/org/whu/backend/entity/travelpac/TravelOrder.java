package org.whu.backend.entity.travelpac;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.whu.backend.entity.accounts.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 实体类 3: Order (订单) - 【需要修改】
 * 职责：关联到具体的出发团期，而不是产品模板。
 */
@Data
@Entity
@SoftDelete
@Table(name = "travel_orders")
public class TravelOrder {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private Integer travelerCount; // 出行人数

    @Column(nullable = false)
    private BigDecimal totalPrice; // 订单总价

    // ... 其他字段保持不变 ...
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status; // 订单状态

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedTime;

    @Column(nullable = false)
    private String contactName;
    @Column(nullable = false)
    private String contactPhone;

    public enum OrderStatus {
        PENDING_PAYMENT, // 待支付
        PAID, // 已支付
        CANCELED, // 取消
        ONGOING, // 正在进行
        COMPLETED // 完成
    }

    // --- 【核心修改】 ---
    // 订单关联的是一个具体的“出发团期”，而不是一个模糊的“产品”
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "departure_id", nullable = false) // 外键从 package_id 改为 departure_id
    private TravelDeparture travelDeparture; // 类型从 TravelPackage 改为 TravelDeparture

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_account_id", referencedColumnName = "id", nullable = false)
    private User user;

    /**
     * 这是一个“虚拟”字段，它不存在于数据库的物理表中。
     * Hibernate会根据这个公式，在每次查询时动态计算出这个字段的值。
     * 我们用CASE语句为不同的订单状态赋予一个优先级数字。
     */
    @Formula("CASE " +
            "  WHEN status = 'PAID' THEN 1 " +
            "  WHEN status = 'PENDING_PAYMENT' THEN 2 " +
            "  WHEN status = 'ONGOING' THEN 3 " +
            "  WHEN status = 'COMPLETED' THEN 4 " +
            "  WHEN status = 'CANCELED' THEN 5 " +
            "  ELSE 99 " +
            "END")
    private int statusPriority;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }
}