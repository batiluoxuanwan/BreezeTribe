package org.whu.backend.entity.Accounts;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;


@Data
@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends Account {
    public Admin() {
        this.setRole(Role.ROLE_ADMIN);
    }
}

