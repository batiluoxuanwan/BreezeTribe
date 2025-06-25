package org.whu.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@SoftDelete
@Table(name = "orders")
public class Order {
    @Id
    @Column(length = 36)
    private String id;

    @Column(length = 36, nullable = false)
    private String userAccountId; // 下单用户 TODO: 改为 @ManyToOne UserAccount

    @Column(length = 36, nullable = false)
    private String packageId; // 关联的旅行团 TODO: 改为 @ManyToOne TravelPackage

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

    public enum OrderStatus {
        PENDING_PAYMENT, // 待支付
        PAID, // 已支付
        CANCELED, // 取消
        COMPLETED // 完成
    }

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null || this.id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        }
    }
}
