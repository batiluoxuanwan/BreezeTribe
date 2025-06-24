package org.whu.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@SoftDelete
@Table(name = "spots")
public class Spot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // 使用UUID作为主键
    private String id;

    @Column(nullable = false)
    private String name; // 景点名称，如“埃菲尔铁塔”
    @Column(nullable = false)
    private String cityName; // 景点所在城市
    @Column(nullable = false)
    private String baiduUid; // 对应百度地图的uid，唯一标识

    private String description; // 描述

    private Double longitude; // 经度
    private Double latitude; // 纬度

    @Column(nullable = false, insertable = false, updatable = false)
    private boolean deleted = false; // 删除标记

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedTime;

}