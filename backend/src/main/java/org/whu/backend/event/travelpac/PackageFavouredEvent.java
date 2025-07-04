package org.whu.backend.event.travelpac;

import lombok.Getter;
import org.whu.backend.entity.accounts.User;

@Getter
public class PackageFavouredEvent {
    private final String packageId;
    private final User user;

    public PackageFavouredEvent(String packageId, User user) {
        this.packageId = packageId;
        this.user = user;
    }
}
