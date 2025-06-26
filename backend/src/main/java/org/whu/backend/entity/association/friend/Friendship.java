//package org.whu.backend.entity.association.friend;
//
//import jakarta.persistence.*;
//import org.springframework.data.annotation.Id;
//import org.whu.backend.entity.accounts.Account;
//
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//@Entity
//@Table(name = "friendships",
//        uniqueConstraints = {@UniqueConstraint(columnNames = {"account1_id", "account2_id"})})
//public class Friendship {
//
//    @Id
//    @Column(length = 36)
//    private String id;
//
//    @ManyToOne
//    @JoinColumn(name = "account1_id", nullable = false)
//    private Account account1;
//
//    @ManyToOne
//    @JoinColumn(name = "account2_id", nullable = false)
//    private Account account2;
//
//    private LocalDateTime createdAt = LocalDateTime.now();
//
//    @PrePersist
//    protected void onCreate() {
//        if (this.id == null || this.id.trim().isEmpty()) {
//            this.id = UUID.randomUUID().toString();
//        }
//    }
//}
