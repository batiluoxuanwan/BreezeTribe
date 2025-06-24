package org.whu.backend.entity;


import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "favorites",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userAccountId", "itemId", "itemType"})) // 一个人对同一个东西只能收藏一次
public class Favorite {
    @Id
    @Column(length = 36)
    private String id;

    @Column(length = 36, nullable = false)
    private String userAccountId; // 谁收藏的 TODO: 改为 @ManyToOne UserAccount

    @Column(length = 36, nullable = false)
    private String itemId; // 收藏的项目的ID (可能是TravelPackage的ID, Spot的ID等)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FavoriteItemType itemType; // 收藏的项目的类型

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdTime;

    public enum FavoriteItemType {
        PACKAGE, // 旅行团
        SPOT,    // 景点
        ROUTE,   // 路线
        POST     // 游记
    }

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null || this.id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        }
    }
}