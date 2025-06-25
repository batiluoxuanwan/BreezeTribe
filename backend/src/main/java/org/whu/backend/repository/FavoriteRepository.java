package org.whu.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.whu.backend.entity.Favorite;
import org.whu.backend.entity.accounts.Merchant;
import org.whu.backend.entity.accounts.User;

import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, String> {
    Optional<Favorite> findByUserAndItemIdAndItemType(User user, String itemId,Enum itemType);
    void deleteByUserAndItemIdAndItemType(User user,String itemId,Enum itemType);
    Page<Favorite> findByUser(User user, Pageable pageable);
    Page<Favorite> findByUserAndItemType(User user,Pageable pageable,Enum itemType);
}
