package org.whu.backend.dto.accounts;

import lombok.Data;

@Data
public class MerchantSummaryDto {
    private String id;
    private String name;
//    //执照
//    private String businessLicenseUrl;
//    //营业许可证
//    private String businessPermitUrl;
//    //身份证(正反两面)
//    private String idCardUrl1;
//    private String idCardUrl2;
    //执照
    private String businessLicense;
    private String companyName;
    private String phone;
    private String email;
}
