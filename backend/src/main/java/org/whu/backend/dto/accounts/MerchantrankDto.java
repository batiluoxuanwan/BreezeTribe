package org.whu.backend.dto.accounts;

import lombok.Data;

@Data
public class MerchantrankDto {
    private String id;
    private String name;
    private String avatarUrl;
    private Double averageRating = 0.0;
}
