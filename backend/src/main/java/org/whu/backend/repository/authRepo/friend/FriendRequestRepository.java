package org.whu.backend.repository.authRepo.friend;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.entity.accounts.Account;
import org.whu.backend.entity.association.friend.FriendRequest;
import org.whu.backend.entity.association.friend.Friendship;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, String> {
    Page<FriendRequest> findByTo_Id(String accountId, Pageable pageable);
    Page<FriendRequest> findByFrom_Id(String accountId, Pageable pageable);

    boolean existsByFromAndTo(Account currentAccount, Account toAccount);
}
