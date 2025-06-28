package org.whu.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.entity.Like;
import org.whu.backend.entity.accounts.User;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, String> {
    Optional<Like> findByUserAndItemIdAndItemType(User user, String itemId, Like.LikeItemType itemType);

    void deleteByUserAndItemIdAndItemType(User user, String itemId, Like.LikeItemType itemType);

    Page<Like> findByUser(User user, Pageable pageable);

    Page<Like> findByUserAndItemType(User user, Pageable pageable, Like.LikeItemType itemType);
}
