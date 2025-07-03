package org.whu.backend.event.travelpac;

import lombok.Getter;
import org.whu.backend.entity.accounts.Account;
import org.whu.backend.entity.travelpac.TravelPackage;

@Getter
public class PackageCommentedEvent {
    private final TravelPackage travelPackage;
    private final Account commentSender;
    private final Account commentReceiver;
    private final String content;

    public PackageCommentedEvent(TravelPackage travelPackage, Account commentSender, Account commentReceiver, String content) {
        this.travelPackage = travelPackage;
        this.commentSender = commentSender;
        this.commentReceiver = commentReceiver;
        this.content = content;
    }
}
