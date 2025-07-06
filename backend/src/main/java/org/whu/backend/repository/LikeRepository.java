package org.whu.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.entity.InteractionItemType;
import org.whu.backend.entity.Like;
import org.whu.backend.entity.accounts.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface LikeRepository extends JpaRepository<Like, String> {
    Optional<Like> findByUserAndItemIdAndItemType(User user, String itemId, InteractionItemType itemType);

    // 批量查询用户对一批特定类型的项目的点赞记录
    List<Like> findByUserIdAndItemTypeAndItemIdIn(String userId, InteractionItemType itemType, Set<String> itemIds);

    void deleteByUserAndItemIdAndItemType(User user, String itemId, InteractionItemType itemType);

    Page<Like> findByUser(User user, Pageable pageable);

    Page<Like> findByUserAndItemType(User user, Pageable pageable, InteractionItemType itemType);

    void deleteAllByItemIdAndItemType(String id,InteractionItemType itemType);
}
