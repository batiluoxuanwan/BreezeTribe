package org.whu.backend.dto.friend;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.whu.backend.entity.accounts.Account;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class FriendDto {
    private String id;
    private Account account1;
    private Account account2;
    private LocalDateTime createdAt;
}
