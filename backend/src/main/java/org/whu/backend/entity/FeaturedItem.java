package org.whu.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 【新增实体】官方推荐条目
 */
@Data
@Entity
@Table(name = "featured_items")
public class FeaturedItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String itemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType itemType;

    @Column(nullable = false)
    private int priority;

    public enum ItemType { PACKAGE, POST }
}