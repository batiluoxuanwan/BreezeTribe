package org.whu.backend.entity;

public class Admin extends Account {
    public Admin() {
        this.setRole(Role.ROLE_ADMIN);
    }
}

