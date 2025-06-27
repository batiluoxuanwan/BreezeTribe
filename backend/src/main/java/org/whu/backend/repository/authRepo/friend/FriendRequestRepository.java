package org.whu.backend.repository.authRepo.friend;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.entity.association.friend.FriendRequest;
import org.whu.backend.entity.association.friend.Friendship;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, String> {
    Page<FriendRequest> findByTo(String accountId, Pageable pageable);
    Page<FriendRequest> findByFrom(String accountId, Pageable pageable);
}
