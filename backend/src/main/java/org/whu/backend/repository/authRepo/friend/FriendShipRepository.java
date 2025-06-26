package org.whu.backend.repository.authRepo.friend;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.entity.association.friend.Friendship;

public interface FriendShipRepository extends JpaRepository<Friendship, String> {
    void deleteByAccountIdAndFriendId(String accountId, String friendId);
    Page<Friendship> findByAccount1(String accountId, Pageable pageable);
}
