package org.whu.backend.entity.accounts;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@DiscriminatorValue("USER")
public class User extends Account {
    //用户专属
    @Lob
    @Column(nullable = true, columnDefinition = "LONGTEXT")
    private String interestProfile; // 存储用户兴趣画像的JSON
}
