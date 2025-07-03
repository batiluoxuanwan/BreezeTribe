package org.whu.backend.event.post;

import lombok.Getter;
import org.whu.backend.entity.travelpost.Comment;

@Getter
public class PostCommentedEvent {
    private final String postId;
    private final Comment comment;

    public PostCommentedEvent(String postId, Comment comment) {
        this.postId = postId;
        this.comment = comment;
    }
}
