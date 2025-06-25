package org.whu.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 单个景点实体
 */
@Data
@Entity
@SoftDelete
@Table(name = "spots")
public class Spot {
    @Id
    @Column(length = 36)
    private String id; // 36位UUID

    @Column(unique = true, nullable = false)
    private String mapProviderUid; // 外部地图供应商的唯一UID，百度地图

    @Column(nullable = false)
    private String name; // 景点名

    private String address; // 地址

    private String city; // 所在城市

    private Double longitude; //经度

    private Double latitude; // 纬度

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdTime; // 创建时间

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedTime; // 更新时间

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null || this.id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        }
    }
}