package org.whu.backend.entity;

public class User extends Account {
    public User() {
        this.setRole(Role.ROLE_USER);
    }
}
