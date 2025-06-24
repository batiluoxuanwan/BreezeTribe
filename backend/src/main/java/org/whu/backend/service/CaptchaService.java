package org.whu.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.whu.backend.common.exception.BizException;

import java.io.File;
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

    public void sendVerificationEmail(String toEmail) {
        String code = generateCode();
        //System.out.println(code);
        log.debug("REIDS里面的值为"+code);
        System.out.println(toEmail);
        //toEmail = toEmail.replace("\"", "");
        // 存Redis，设置过期时间5分钟
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + toEmail, code, 5, TimeUnit.MINUTES);
        try {

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("2365246549@qq.com");
            helper.setTo(toEmail);
            helper.setSubject("【BreezeTribe】注册验证码");

            // ✅ 构建HTML内容
            String htmlContent = """
                <html>
                <body style="font-family: Arial, sans-serif; background-color: #f9fafb; margin: 0; padding: 0;">
                  <table role="presentation" cellpadding="0" cellspacing="0" width="100%%" style="margin: 0 auto; padding: 40px 0;">
                    <tr>
                      <td align="center">
                        <table role="presentation" cellpadding="0" cellspacing="0" width="600" style="background-color: #ffffff; border-radius: 8px; padding: 40px; box-shadow: 0px 4px 15px rgba(0,0,0,.1);">
                          
                          <!-- Logo / Header -->
                          <tr>
                            <td align="center" style="font-size: 24px; font-weight: bold; color: #4CAF50;">
                              【风旅集】
                            </td>
                          </tr>
                
                          <!-- Message -->
                          <tr>
                            <td style="font-size: 16px; color: #333333; padding: 30px 0 10px;">
                              您正在注册 BreezeTribe，您的验证码是：
                            </td>
                          </tr>
                          
                          <!-- Code Box -->
                          <tr>
                            <td align="center" style="padding: 10px 0;">
                              <div style="
                                font-size: 32px;
                                font-weight: bold;
                                color: #007BFF;
                                background-color: #f1f3f5;
                                padding: 20px 30px;
                                border-radius: 8px;
                                user-select: all;">
                                %s
                              </div>
                            </td>
                          </tr>
                
                          <!-- Validity Info -->
                          <tr>
                            <td style="font-size: 14px; color: #555555; text-align: center; padding: 10px 0;">
                              ⚡ 有效期 5 分钟，请尽快验证。
                            </td>
                          </tr>
                
                          <!-- Button -->
                          <tr>
                            <td align="center" style="padding: 20px 0;">
                              <a href="https://www.yourappurl.com/verify?code=%s&email=%s" 
                                style="
                                  font-size: 16px;
                                  font-weight: bold;
                                  color: #ffffff;
                                  background-color: #007BFF;
                                  padding: 14px 28px;
                                  border-radius: 5px;
                                  text-decoration: none;">
                                点击跳转验证
                              </a>
                            </td>
                          </tr>
                
                          <!-- Fallback Info -->
                          <tr>
                            <td style="font-size: 14px; color: #777777; text-align: center; padding: 10px 0;">
                              如果以上链接无效，您也可以手动将验证码复制到应用中。
                            </td>
                          </tr>
                
                          <!-- Logo Image -->
                          <tr>
                            <td align="center" style="padding: 30px 0;">
                              <img src="cid:logoImage" alt="BreezeTribe Logo" style="width: 100px; height: auto;" />
                            </td>
                          </tr>
                
                          <!-- Signature -->
                          <tr>
                            <td style="font-size: 12px; color: #888888; text-align: center;">
                              —— BreezeTribe 团队
                            </td>
                          </tr>
                
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(code, code, toEmail);



            helper.setText(htmlContent, true);

            // ✅ 嵌入本地图片
            ClassPathResource resource = new ClassPathResource("pic/nailong.jpg");
            helper.addInline("logoImage", resource);

            // ✅ 发送邮件
            mailSender.send(mimeMessage);
        }
        catch (MessagingException e) {
            log.error("发送邮件失败", e);
            e.printStackTrace();
            throw new BizException("邮件发送失败");
        }
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
