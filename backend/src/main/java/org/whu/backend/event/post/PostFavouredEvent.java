package org.whu.backend.event.post;

import lombok.Getter;
import org.whu.backend.entity.travelpost.TravelPost;

@Getter
public class PostFavouredEvent {
    private final TravelPost post;
    private final String triggeredUserId;

    public PostFavouredEvent(TravelPost post, String triggeredUserId) {
        this.post = post;
        this.triggeredUserId = triggeredUserId;
    }
}
