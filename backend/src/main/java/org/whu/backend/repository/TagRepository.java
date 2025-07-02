package org.whu.backend.repository;


import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.whu.backend.entity.Tag;

import java.util.List;
import java.util.Optional;

/**
 * 标签实体的Repository
 */
public interface TagRepository extends JpaRepository<Tag, String> {

    /**
     * 根据名称查找标签（用于查重）
     */
    Optional<Tag> findByName(String name);

    /**
     * 根据分类查找标签列表
     */
    List<Tag> findByCategory(Tag.TagCategory category);

    /**
     * 检查一个标签是否被任何产品或游记使用
     * @param tagId 标签ID
     * @return 如果被使用则返回true，否则返回false
     */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM package_tags WHERE tag_id = :tagId LIMIT 1)", nativeQuery = true) // 假设游记标签表叫 post_tags
    // @Query(value = "SELECT EXISTS(SELECT 1 FROM package_tags WHERE tag_id = :tagId UNION ALL SELECT 1 FROM post_tags WHERE tag_id = :tagId)", nativeQuery = true)
    boolean isTagInUse(@Param("tagId") String tagId);

    Page<Tag> findByCategory(Tag.TagCategory category, Pageable pageable);
}