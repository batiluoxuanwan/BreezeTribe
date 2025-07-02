package org.whu.backend.repository.authRepo.friend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.whu.backend.entity.association.friend.Message;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, String> {

    @Query("SELECT m FROM Message m WHERE " +
            "(m.fromAccountId = :userId AND m.toAccountId = :friendId) OR " +
            "(m.fromAccountId = :friendId AND m.toAccountId = :userId) " +
            "ORDER BY m.timestamp ASC")
    List<Message> findByFromAccountIdAndToAccountIdOrViceVersa(
            @Param("userId") String userId, @Param("friendId") String friendId);

    List<Message> findByFromAccountIdAndToAccountId(String accountId, String friendId);
}

