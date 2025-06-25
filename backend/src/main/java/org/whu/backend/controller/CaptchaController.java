package org.whu.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.whu.backend.common.Result;
import org.whu.backend.service.CaptchaService;

@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    @Autowired
    private CaptchaService captchaService;

    @PostMapping("/sendEmail")
    public Result sendEmail(@RequestParam String email) {
        captchaService.sendVerificationEmail(email);
        return Result.success("验证码已发送，请查收邮件");
    }




}
