package org.whu.backend.entity.Accounts;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;


@Data
@Entity
@DiscriminatorValue("MERCHANT")
public class Merchant extends Account {
    public Merchant() {
        this.setRole(Role.ROLE_MERCHANT);
    }
    //营业执照
    private String businessLicenseUrl;
    //营业许可证
    private String businessPermitUrl;
    //身份证(正反两面)
    private String idCardUrl1;
    private String idCardUrl2;

    private boolean approved = false;   // 审核状态

}
