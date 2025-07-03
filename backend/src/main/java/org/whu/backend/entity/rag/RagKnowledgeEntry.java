package org.whu.backend.entity.rag;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "rag_knowledge_base")
public class RagKnowledgeEntry {

    @Id
    @Column(length = 36)
    private String id; // 直接使用TravelPackage的ID作为主键

    @Column(nullable = false)
    private String name; // 标题

    @Column(nullable = false)
    private BigDecimal startingPrice; // 起步价

    private Integer durationInDays; // 持续天数

    @Lob
    @Column(nullable = false,columnDefinition = "TEXT")
    private String content; // 整合后的长文本内容
}