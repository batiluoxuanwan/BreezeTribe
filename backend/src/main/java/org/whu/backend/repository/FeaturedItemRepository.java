package org.whu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.entity.FeaturedItem;

import java.util.List;

/**
 * 【新增】官方推荐条目的Repository
 */
interface FeaturedItemRepository extends JpaRepository<FeaturedItem, Long> {
    List<FeaturedItem> findAllByOrderByPriorityDesc();
}
