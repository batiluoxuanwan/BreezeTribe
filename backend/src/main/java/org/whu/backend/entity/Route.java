package org.whu.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@SoftDelete
@Table(name = "routes")
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name; // 路线名称，如“巴黎艺术一日游”

    // 一条路线包含多个景点，是多对多关系
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "route_spots",
            joinColumns = @JoinColumn(name = "route_id"),
            inverseJoinColumns = @JoinColumn(name = "spot_id"))
    private List<Spot> spots = new ArrayList<>();

    // 这条路线是哪个经销商创建的
    private String dealerAccountId; // TODO: 后续关联到经销商Account实体

    @Column(nullable = false, insertable = false, updatable = false)
    private boolean deleted = false; // 删除标记

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedTime;
}