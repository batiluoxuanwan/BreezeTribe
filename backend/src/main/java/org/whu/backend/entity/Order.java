package org.whu.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.whu.backend.entity.accounts.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 表示一个用户创建的加入旅游团的订单
 */
// 已经废弃！！！！！！！！！！！！！！！！！！！！！！！！
@Data
@Entity
@SoftDelete
@Table(name = "orders")
public class Order {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private Integer travelerCount; // 出行人数

    @Column(nullable = false)
    private BigDecimal totalPrice; // 订单总价

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

    // 实体关联，表示订单所属的用户账号
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_account_id", referencedColumnName = "id", nullable = false)
    private User user;

    // 实体关联，该订单对应的旅行团
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "package_id", nullable = false)
    private TravelPackage travelPackage;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null || this.id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        }
    }
}
