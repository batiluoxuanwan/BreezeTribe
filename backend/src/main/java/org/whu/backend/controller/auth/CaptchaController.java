package org.whu.backend.controller.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.service.auth.CaptchaService;
import org.whu.backend.util.JpaUtil;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    @Autowired
    private CaptchaService captchaService;

    @PostMapping("/sendEmail")
    public Result<?> sendEmail(@RequestParam String email) {
        log.info("发送验证码请求，邮箱: {}", email);
        captchaService.sendVerificationEmail(email);
        return Result.success("验证码已发送，请查收邮件");
    }
    @PostMapping("/resetByEmail")
    public Result<?> resetEmail(@RequestParam String email) {
        log.info("发送验证码请求，邮箱: {}", email);
        captchaService.resetVerificationEmail(email);
        return Result.success("验证码已发送，请查收邮件");
    }
    @PostMapping("/sendSms")
    public Result<?> sendSms(@RequestParam String phone) {
        log.info("发送验证码请求，手机号: {}", phone);
        captchaService.sendVerificationSms(phone);
        return Result.success("验证码已发送，请查收短信");
    }
    @PostMapping("/resetBySms")
    public Result<?> resetSms(@RequestParam String phone)  {
        log.info("发送验证码请求，手机号: {}", phone);
        captchaService.resetVerificationSms(phone);
        return Result.success("验证码已发送，请查收短信");
    }

    @GetMapping("/slider")
    public Result<?> getSliderCaptcha() throws IOException {
        return Result.success(captchaService.generateSliderCaptcha());
    }

    @PostMapping("/verify")
    public Result<?> verifySliderCaptcha(@RequestBody Map<String, Object> data) {
        String captchaId = (String) data.get("captchaId");
        Integer userX = (Integer) data.get("sliderX");

        boolean result = captchaService.verifySliderCaptcha(captchaId, userX);
        JpaUtil.isTrue(result, "验证失败");
        return Result.success("验证通过");
    }
    @PostMapping("/bindEmail")
    public Result<?> bindEmail(@RequestParam String email) {
        captchaService.resetEmail(email);
        return Result.success("验证码已发送，请查收邮件");
    }
    @PostMapping("/bindSms")
    public Result<?> bindOrUpdatePhone(@RequestParam String phone) {
        captchaService.resetPhone(phone);
        return Result.success("验证码已发送，请查收邮件");
    }
}
