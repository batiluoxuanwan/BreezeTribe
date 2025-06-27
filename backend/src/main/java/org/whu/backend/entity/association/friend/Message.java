package org.whu.backend.entity.association.friend;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String fromAccountId;

    @Column(nullable = false)
    private String toAccountId;

    private String content;

    private LocalDateTime timestamp;

    private boolean hadread;

    // 预留字段：图片/语音/撤回等
}

