package org.whu.backend.event.post;

import lombok.Getter;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.entity.travelpost.TravelPost;

@Getter // 使用Lombok
public class PostLikedEvent {
    private final TravelPost post;
    private final User user;

    public PostLikedEvent(TravelPost post, User user) {
        this.post = post;
        this.user = user;
    }
}