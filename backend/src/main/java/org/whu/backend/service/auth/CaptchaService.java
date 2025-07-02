package org.whu.backend.service.auth;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.util.JpaUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.aspectj.weaver.tools.cache.SimpleCacheFactory.path;

@Slf4j
@Service
public class CaptchaService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${sms.host}")
    private String host;

    @Value("${sms.path}")
    private String path;

    @Value("${sms.method}")
    private String method;

    @Value("${sms.appcode}")
    private String appcode;

    @Value("${sms.smsSignId}")
    private String smsSignId;

    @Value("${sms.templateId}")
    private String templateId;

    @Value("${spring.mail.username}")
    private String from;


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
            redisTemplate.opsForValue().set("email:" + toEmail, code, 5, TimeUnit.MINUTES);

            String title = "您正在注册 BreezeTribe，您的验证码为：";
            String url = "https://yourdomain.com/verify?code=" + code + "&email=" + toEmail;

            sendEmail(toEmail, "【风旅集】注册验证码", title, title, code, url);
    }


    public void resetVerificationEmail(String email) {
        String code = generateCode();
        redisTemplate.opsForValue().set("email:" + email, code, 5, TimeUnit.MINUTES);

        String title = "您正在重置 BreezeTribe 密码，您的验证码为：";
        String url = "https://yourdomain.com/reset?code=" + code + "&email=" + email;

        sendEmail(email, "【风旅集】重置密码验证码", title, title, code, url);
    }

    // 校验验证码
    public boolean verifyEmailCode(String email, String code) {
        String key = "email:" + email;
        String cachedCode = redisTemplate.opsForValue().get(key);

        if (cachedCode != null && cachedCode.equals(code)) {
            // 验证通过后删除验证码（可选）
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }
    private void sendAliyunSms(String mobile, String code, int minutes) {
        String url = host + path +
                "?mobile=" + mobile +
                "&param=" + "**code**:" + code + ",**minute**:" + minutes +
                "&smsSignId=" + smsSignId +
                "&templateId=" + templateId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "APPCODE " + appcode);

        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new BizException("短信发送失败，请稍后再试");
        }
    }
    public void sendVerificationSms(String mobile) {
            JpaUtil.notNull(mobile, "手机号不能为空");

//            String rateLimitKey = "sms:rate:" + mobile;
//            if (Boolean.TRUE.equals(redisTemplate.hasKey(rateLimitKey))) {
//                throw new BizException("验证码发送过于频繁，请稍后再试");
//            }
        int minutes = 5;
        String code = generateCode(); // 6位验证码
        // 发送短信（复用封装方法）
        sendAliyunSms(mobile, code, minutes);
        // 设置验证码及频率限制
        //redisTemplate.opsForValue().set(rateLimitKey, "1", 60, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set("sms:" + mobile, code, 5, TimeUnit.MINUTES);
    }

    public void resetVerificationSms(String phone) {
        JpaUtil.notNull(phone, "手机号不能为空");

        int minutes = 5;
        String code = generateCode(); // 通用6位验证码生成方法

        // 1. 发送短信（调用复用方法）
        sendAliyunSms(phone, code, minutes);

        // 2. 存入 Redis（用于后续验证）
        redisTemplate.opsForValue().set("sms:" + phone, code, minutes, TimeUnit.MINUTES);
    }
    public boolean verifySmsCode(String phone, String code) {
        String key = "sms:" + phone;
        String cachedCode = redisTemplate.opsForValue().get(key);

        if (cachedCode != null && cachedCode.equals(code)) {
            // 验证通过后删除验证码（可选）
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    private final int width = 280, height = 200, blockSize = 50;

    public Map<String, String> generateSliderCaptcha() throws IOException {

        BufferedImage bgImg = loadRandomBackgroundImage();

        int x = new Random().nextInt(width - blockSize - 20) + 10;
        int y = new Random().nextInt(height - blockSize - 20) + 10;

        BufferedImage block = bgImg.getSubimage(x, y, blockSize, blockSize);

        Graphics2D g2d = bgImg.createGraphics();
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(x, y, blockSize, blockSize);
        g2d.dispose();

        String bgBase64 = encodeBase64(bgImg);
        String blockBase64 = encodeBase64(block);

        String captchaId = UUID.randomUUID().toString();
        String X=""+x;
        redisTemplate.opsForValue().set("slider:" + captchaId, X, 2, TimeUnit.MINUTES);

        return Map.of(
                "captchaId", captchaId,
                "bgImage", bgBase64,
                "blockImage", blockBase64
        );
    }

    public boolean verifySliderCaptcha(String captchaId, Integer userX) {
        String key = "slider:" + captchaId;
        Integer trueX = Integer.valueOf(redisTemplate.opsForValue().get(key));

        if (trueX == null) return false;

        redisTemplate.delete(key);
        int tolerance = 5;
        return Math.abs(userX - trueX) <= tolerance;
    }

    private String encodeBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
    private BufferedImage loadRandomBackgroundImage() throws IOException {
        String[] imageNames = { "1.jpg", "2.jpg", "3.jpg", "4.jpg", "5.jpg" };
        int index = new Random().nextInt(imageNames.length);
        String selected = imageNames[index];

        return ImageIO.read(getClass().getResourceAsStream("/pic/" + selected));
    }

    public void resetEmail(String newEmail) {
        String code = generateCode();
        redisTemplate.opsForValue().set("email:" + newEmail, code, 5, TimeUnit.MINUTES);

        String title = "您正在绑定邮箱 BreezeTribe，您的验证码为：";
        String url = "https://yourdomain.com/verify?code=" + code + "&email=" + newEmail;

        sendEmail(newEmail, "【风旅集】绑定验证码", title, title, code, url);
    }

    public void resetPhone(String newPhone) {
        JpaUtil.notNull(newPhone, "手机号不能为空");
        int minutes = 5;
        String code = generateCode(); // 6位验证码
        // 发送短信（复用封装方法）
        sendAliyunSms(newPhone, code, minutes);
        // 设置验证码及频率限制
        redisTemplate.opsForValue().set("sms:" + newPhone, code, 5, TimeUnit.MINUTES);
    }
}
