package org.whu.backend.entity;

public class Merchant extends Account {
    public Merchant() {
        this.setRole(Role.ROLE_MERCHANT);
    }
}
