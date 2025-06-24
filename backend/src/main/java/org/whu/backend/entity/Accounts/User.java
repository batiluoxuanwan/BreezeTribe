package org.whu.backend.entity.Accounts;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;

@Data
@Entity
@DiscriminatorValue("USER")
public class User extends Account {
    //用户专属
}
