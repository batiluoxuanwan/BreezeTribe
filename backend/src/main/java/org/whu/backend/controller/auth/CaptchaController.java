package org.whu.backend.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(summary = "发送注册验证码到邮箱", description = "向指定邮箱发送注册/登录验证码")
    @PostMapping("/sendEmail")
    public Result<?> sendEmail(
            @Parameter(description = "目标邮箱地址") @RequestParam String email) {
        log.info("发送验证码请求，邮箱: {}", email);
        captchaService.sendVerificationEmail(email);
        return Result.success("验证码已发送，请查收邮件");
    }

    @Operation(summary = "发送重置密码验证码到邮箱", description = "向指定邮箱发送用于重置密码的验证码")
    @PostMapping("/resetByEmail")
    public Result<?> resetEmail(
            @Parameter(description = "目标邮箱地址") @RequestParam String email) {
        log.info("发送验证码请求，邮箱: {}", email);
        captchaService.resetVerificationEmail(email);
        return Result.success("验证码已发送，请查收邮件");
    }

    @Operation(summary = "发送注册验证码到手机", description = "向指定手机号发送注册/登录验证码")
    @PostMapping("/sendSms")
    public Result<?> sendSms(
            @Parameter(description = "目标手机号") @RequestParam String phone) {
        log.info("发送验证码请求，手机号: {}", phone);
        captchaService.sendVerificationSms(phone);
        return Result.success("验证码已发送，请查收短信");
    }

    @Operation(summary = "发送重置密码验证码到手机", description = "向指定手机号发送用于重置密码的验证码")
    @PostMapping("/resetBySms")
    public Result<?> resetSms(
            @Parameter(description = "目标手机号") @RequestParam String phone) {
        log.info("发送验证码请求，手机号: {}", phone);
        captchaService.resetVerificationSms(phone);
        return Result.success("验证码已发送，请查收短信");
    }

    @Operation(summary = "获取滑动验证码图片", description = "生成滑动验证所需的图像和验证码 ID")
    @GetMapping("/slider")
    public Result<?> getSliderCaptcha() throws IOException {
        return Result.success(captchaService.generateSliderCaptcha());
    }

    @Operation(summary = "校验滑动验证码", description = "前端完成滑动后传入位置与 ID 进行校验")
    @PostMapping("/verify")
    public Result<?> verifySliderCaptcha(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "包含 captchaId 和用户滑动位置 sliderX",
                    required = true
            )
            @RequestBody Map<String, Object> data) {
        String captchaId = (String) data.get("captchaId");
        Integer userX = (Integer) data.get("sliderX");

        boolean result = captchaService.verifySliderCaptcha(captchaId, userX);
        JpaUtil.isTrue(result, "验证失败");
        return Result.success("验证通过");
    }

    @Operation(summary = "发送绑定邮箱验证码", description = "向新邮箱地址发送验证码以完成绑定或更换")
    @PostMapping("/bindEmail")
    public Result<?> bindEmail(
            @Parameter(description = "目标邮箱地址") @RequestParam String email) {
        captchaService.resetEmail(email);
        return Result.success("验证码已发送，请查收邮件");
    }

    @Operation(summary = "发送绑定手机验证码", description = "向新手机号发送验证码以完成绑定或更换")
    @PostMapping("/bindSms")
    public Result<?> bindOrUpdatePhone(
            @Parameter(description = "目标手机号") @RequestParam String phone) {
        captchaService.resetPhone(phone);
        return Result.success("验证码已发送，请查收短信");
    }
}
