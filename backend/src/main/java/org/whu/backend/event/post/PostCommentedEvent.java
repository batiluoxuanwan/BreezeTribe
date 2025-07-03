package org.whu.backend.event.post;

import lombok.Getter;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.entity.travelpost.TravelPost;

@Getter
public class PostCommentedEvent {
    private final TravelPost post;
    private final User commentSender;
    private final User commentReceiver;
    private final String content;

    public PostCommentedEvent(TravelPost post, User commentSender, User commentReceiver, String content) {
        this.post = post;
        this.commentSender = commentSender;
        this.commentReceiver = commentReceiver;
        this.content = content;
    }
}
