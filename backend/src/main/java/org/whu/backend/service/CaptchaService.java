package org.whu.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.whu.backend.common.exception.BizException;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CaptchaService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String CODE_KEY_PREFIX = "email:code:";


    // 生成6位数字验证码
    public String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private void sendEmail(String toEmail, String subject, String title, String description,
                           String code, String buttonUrl) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("2365246549@qq.com");
            helper.setTo(toEmail);
            helper.setSubject(subject);

            String htmlContent = """
            <html>
            <body style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background:#f5f7fa; margin:0; padding:0;">
              <table align="center" width="600" style="background:#fff; border-radius:12px; padding:40px 48px; box-shadow:0 8px 24px rgba(0,0,0,0.12);">
                <tr><td align="center" style="font-size: 30px; font-weight: 700; color:#2a9d8f; padding-bottom: 24px;">
                    【 风旅集 】</td></tr>
                <tr><td style="font-size: 17px; color: #444; text-align: center; padding-bottom: 32px; line-height: 1.6;">
                    %s
                </td></tr>
                <tr><td align="center" style="padding-bottom: 40px;">
                      <div style="display:inline-block; font-size: 42px; font-weight: 800; color:#264653;
                           background:#e7f0fd; padding: 24px 56px; border-radius: 16px; letter-spacing: 14px; user-select: all;">
                      %s
                      </div>
                </td></tr>
                <tr><td align="center" style="padding-bottom: 36px;">
                      <a href="%s" style="
                        display:inline-block;
                        padding: 16px 36px;
                        font-size: 18px;
                        font-weight: 700;
                        color: #fff;
                        background-color: #2a9d8f;
                        border-radius: 12px;
                        box-shadow: 0 10px 20px rgba(42, 157, 143, 0.38);
                        text-decoration: none;">
                        点击跳转
                      </a>
                </td></tr>
                <tr><td align="center" style="font-size: 14px; color: #777; padding-bottom: 32px;">
                      如果按钮无法点击，请复制验证码手动输入
                </td></tr>
                <tr><td align="center" style="padding-bottom: 12px;">
                      <img src="cid:logoImage" alt="BreezeTribe Logo" style="width: 90px; height: auto;" />
                </td></tr>
                <tr><td align="center" style="font-size: 12px; color: #aaa;">
                      —— BreezeTribe 团队敬上
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(description, code, buttonUrl);

            helper.setText(htmlContent, true);
            ClassPathResource resource = new ClassPathResource("pic/nailong.jpg");
            helper.addInline("logoImage", resource);

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            log.error("发送邮件失败", e);
            throw new BizException("邮件发送失败");
        }
    }

    public void sendVerificationEmail(String toEmail) {
            String code = generateCode();
            redisTemplate.opsForValue().set(CODE_KEY_PREFIX + toEmail, code, 5, TimeUnit.MINUTES);

            String title = "您正在注册 BreezeTribe，您的验证码为：";
            String url = "https://yourdomain.com/verify?code=" + code + "&email=" + toEmail;

            sendEmail(toEmail, "【风旅集】注册验证码", title, title, code, url);
    }


    public void resetVerificationEmail(String email) {
        String code = generateCode();
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + email, code, 5, TimeUnit.MINUTES);

        String title = "您正在重置 BreezeTribe 账号密码，您的验证码为：";
        String url = "https://yourdomain.com/reset?code=" + code + "&email=" + email;

        sendEmail(email, "【风旅集】重置密码验证码", title, title, code, url);
    }

    // 校验验证码
    public boolean verifyCode(String email, String code) {
        String key = CODE_KEY_PREFIX + email;
        String cachedCode = redisTemplate.opsForValue().get(key);

        if (cachedCode != null && cachedCode.equals(code)) {
            // 验证通过后删除验证码（可选）
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }
}
