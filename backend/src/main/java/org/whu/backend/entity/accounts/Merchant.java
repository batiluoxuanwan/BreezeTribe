package org.whu.backend.entity.accounts;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;


@Data
@Entity
@DiscriminatorValue("MERCHANT")
public class Merchant extends Account {
    //营业执照
    private String businessLicense;
    //营业许可证
    private String businessPermitUrl;
    //身份证(正反两面)
    private String idCardUrl1;
    private String idCardUrl2;
    private String companyName;
    private String rejectionReason;
    // 审核状态
    public enum status {
        APPROVED, REJECTED, PENDING
    }

    private Double averageRating = 0.0;


    @Enumerated(EnumType.STRING)
    private status approval=status.PENDING;

}
