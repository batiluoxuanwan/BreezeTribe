package org.whu.backend.entity.accounts;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;


@Data
@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends Account {
    //管理专属
    private boolean approved = false;   // 审核状态
}

