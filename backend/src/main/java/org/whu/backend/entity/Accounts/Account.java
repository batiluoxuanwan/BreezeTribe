package org.whu.backend.entity.Accounts;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "accounts",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"email", "role"}),
                @UniqueConstraint(columnNames = {"phone", "role"})
        }
)
@Inheritance(strategy = InheritanceType.JOINED) //使用 Joined 策略
//@Table(name = "accounts")
@DiscriminatorColumn(name = "account_type", discriminatorType = DiscriminatorType.STRING)
@Data
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String email;

    private String phone;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String username;

    private String avatarUrl;

    private boolean enabled = true;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    // 在保存和更新时自动修改时间
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
