package org.whu.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.whu.backend.entity.accounts.Merchant;
import org.whu.backend.entity.association.RouteSpot;

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
    @Column(columnDefinition = "TEXT")
    private String description; // 路线描述

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedTime;

    // 字段类型是具体的子类，但JoinColumn指向父类的表和主键
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dealer_account_id", referencedColumnName = "id", nullable = false)
    private Merchant dealer;

    // 使用关系表RouteSpot关联景点与路线
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderColumn ASC") // 正确地按照关联实体中的字段排序
    private List<RouteSpot> spots = new ArrayList<>();

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null || this.id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        }
    }
}