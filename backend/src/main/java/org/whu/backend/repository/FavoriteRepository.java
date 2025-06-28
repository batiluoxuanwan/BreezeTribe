package org.whu.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.entity.Favorite;
import org.whu.backend.entity.InteractionItemType;
import org.whu.backend.entity.accounts.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FavoriteRepository extends JpaRepository<Favorite, String> {
    Optional<Favorite> findByUserAndItemIdAndItemType(User user, String itemId, InteractionItemType itemType);

    // 批量查询用户对一批特定类型的项目的收藏记录
    List<Favorite> findByUserIdAndItemTypeAndItemIdIn(String userId, InteractionItemType itemType, Set<String> itemIds);

    void deleteByUserAndItemIdAndItemType(User user, String itemId, InteractionItemType itemType);

    Page<Favorite> findByUser(User user, Pageable pageable);

    Page<Favorite> findByUserAndItemType(User user, Pageable pageable, InteractionItemType itemType);
}
