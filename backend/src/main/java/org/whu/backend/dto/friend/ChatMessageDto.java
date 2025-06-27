package org.whu.backend.dto.friend;

import lombok.Data;

@Data
public class ChatMessageDto {
    private String from;
    private String to;
    private String content;
}
