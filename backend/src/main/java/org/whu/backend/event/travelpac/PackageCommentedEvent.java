package org.whu.backend.event.travelpac;

import lombok.Getter;
import org.whu.backend.entity.travelpost.Comment;

@Getter
public class PackageCommentedEvent {
    private final String packageId;
    private final Comment comment;

    public PackageCommentedEvent(String packageId, Comment comment) {
        this.packageId = packageId;
        this.comment = comment;
    }
}
