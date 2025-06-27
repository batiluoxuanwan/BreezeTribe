package org.whu.backend.repository.authRepo.friend;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.entity.accounts.Account;
import org.whu.backend.entity.association.friend.Friendship;

public interface FriendShipRepository extends JpaRepository<Friendship, String> {
    void deleteByAccount1AndAccount2(Account account1, Account account2);
    Page<Friendship> findByAccount1(Account account1, Pageable pageable);
}
