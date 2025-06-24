package org.whu.backend.util;

import lombok.Data;


@Data
public class CaptchaData {
    private String captchaId;
    private String backgroundImage;
    private String sliderImage;
    private int targetX;

    public CaptchaData(String captchaId, String backgroundImage, String sliderImage, int targetX) {
        this.captchaId = captchaId;
        this.backgroundImage = backgroundImage;
        this.sliderImage = sliderImage;
        this.targetX = targetX;
    }
}
