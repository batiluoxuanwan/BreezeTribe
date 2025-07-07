package org.whu.backend.dto.friend;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.whu.backend.dto.accounts.AuthorDto;
import org.whu.backend.entity.accounts.Account;
import org.whu.backend.entity.association.friend.FriendRequest;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class FriendRequestDto {
    private String id;
    private AuthorDto from;
    private AuthorDto to;
    private FriendRequest.RequestStatus status;
    private LocalDateTime createdAt;
}
