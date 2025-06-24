package org.whu.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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
        // 存Redis，设置过期时间5分钟
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + toEmail, code, 5, TimeUnit.MINUTES);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("2365246549@qq.com");  // 注意改成你的发件邮箱
        message.setTo(toEmail);
        message.setSubject("【BreezeTribe】注册验证码");
        message.setText("您的验证码是：" + code + "，有效期5分钟，请尽快验证。");

        mailSender.send(message);
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
