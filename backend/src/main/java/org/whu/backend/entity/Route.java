package org.whu.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 单个路线实体，一个路线实体可以有很多个景点
 */
@Data
@Entity
@SoftDelete
@Table(name = "routes")
public class Route {
    @Id
    @Column(length = 36)
    private String id; // 36位UUID

    @Column(nullable = false)
    private String name; // 路线名字

    @Lob
    private String description; // 路线描述

//    // 该路线所属的经销商 // TODO: 等Account表实现
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "dealer_account_id", nullable = false)
//    private DealerAccount dealer;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "route_spots",
            joinColumns = @JoinColumn(name = "route_id"),
            inverseJoinColumns = @JoinColumn(name = "spot_id"))
    @OrderBy("order_column ASC")
    private List<Spot> spots = new ArrayList<>(); // 多对多关系，Route与Spot

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedTime;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null || this.id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        }
    }
}