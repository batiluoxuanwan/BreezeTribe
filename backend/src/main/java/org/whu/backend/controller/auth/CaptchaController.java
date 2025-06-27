package org.whu.backend.controller.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.service.auth.CaptchaService;

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
    @PostMapping("/resetEmail")
    public Result<?> resetEmail(@RequestParam String email) {
        log.info("发送验证码请求，邮箱: {}", email);
        captchaService.resetVerificationEmail(email);
        return Result.success("验证码已发送，请查收邮件");
    }
//    @PostMapping("/sendSms")
//    public Result<?> sendSms(@RequestParam String phone) {
//        log.info("发送验证码请求，手机号: {}", phone);
//        captchaService.sendVerificationSms(phone);
//        return Result.success("验证码已发送，请查收短信");
//    }
//    @PostMapping("/resetSms")
//    public Result<?> resetSms(@RequestParam String phone)  {
//        log.info("发送验证码请求，手机号: {}", phone);
//        captchaService.resetVerificationSms(phone);
//        return Result.success("验证码已发送，请查收短信");
//    }
}
