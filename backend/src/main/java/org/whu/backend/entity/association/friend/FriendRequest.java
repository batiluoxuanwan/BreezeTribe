package org.whu.backend.entity.association.friend;

import jakarta.persistence.*;
import lombok.Data;
import jakarta.persistence.Id;
import org.whu.backend.entity.accounts.Account;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "friend_requests",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"from_account_id", "to_account_id"})})
public class FriendRequest {
    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account from;

    @ManyToOne
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account to;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (this.id == null || this.id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();

    }

    public enum RequestStatus {
        PENDING,
        ACCEPTED,
        REJECTED
    }
}

