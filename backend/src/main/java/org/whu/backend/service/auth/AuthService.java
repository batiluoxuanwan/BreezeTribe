package org.whu.backend.service.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.whu.backend.common.Result;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.auth.*;
import org.whu.backend.entity.accounts.*;
import org.whu.backend.repository.authRepo.AuthRepository;
import org.whu.backend.util.AccountUtil;
import org.whu.backend.util.AliyunOssUtil;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.whu.backend.entity.accounts.Merchant.status.PENDING;
import static org.whu.backend.service.DtoConverter.IMAGE_PROCESS;

@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final CaptchaService captchaService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisTemplate<String, String> redisTemplate;
    @Autowired
    private AccountUtil accountUtil;

    public AuthService(AuthRepository authRepository,
                       CaptchaService captchaService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RedisTemplate<String, String> redisTemplate
    ) {
        this.authRepository = authRepository;
        this.captchaService = captchaService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
    }

    // 生成名字后缀
    public String generatename() {
        Random random = new Random();
        int code = 1000 + random.nextInt(9000);
        return String.valueOf(code);
    }

    // 注册
    public Result<?> register(RegisterRequest request) {
        String email = request.getEmail();
        String phone = request.getPhone();
        Role role = request.getRole();
        String code = request.getCode(); //验证码字段

        if ((email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
            throw new BizException("邮箱或手机号必须提供其中之一");
        }
        if (!isValidEmail(email) && !isValidPhone(phone)) {
            throw new BizException("账号必须是有效的邮箱或手机号");
        }

        boolean exists = false;
        if (email != null && !email.isBlank()) {
            if (!captchaService.verifyEmailCode(email, code))
                throw new BizException("邮箱验证码错误");
            exists = authRepository.existsByEmailAndRole(email, role);
        } else {
            if (!captchaService.verifySmsCode(phone, code))
                throw new BizException("手机验证码错误");
            exists = authRepository.existsByPhoneAndRole(phone, role);
        }
        if (exists) {
            throw new BizException((email != null && !email.isBlank()) ? "该邮箱已被注册" : "该手机号已被注册");
        }

        Account account;
        switch (role) {
            case ROLE_USER:
                User user = new User();
                user.setRole(Role.ROLE_USER);
                user.setUsername("新用户" + generatename());
                //user.setEnabled(true);
                user.setBanDurationDays(0);
                account = user;
                break;
            case ROLE_MERCHANT:
                Merchant merchant = new Merchant();
                merchant.setRole(Role.ROLE_MERCHANT);
                merchant.setUsername("新商家" + generatename());
                //merchant.setEnabled(false);
                merchant.setBanDurationDays(-1);
                merchant.setApproval(PENDING);
                // 特有字段暂时不填，后续用接口补充
                account = merchant;
                break;
            case ROLE_ADMIN:
                Admin admin = new Admin();
                admin.setRole(Role.ROLE_ADMIN);
                admin.setUsername("管理员" + generatename());
                //admin.setEnabled(false);
                admin.setBanDurationDays(-1);
                // 特有字段暂时不填，后续用接口补充
                account = admin;
                break;
            default:
                throw new BizException("未知角色");
        }
        account.setEmail(email);
        account.setPhone(phone);
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        long timestamp = System.currentTimeMillis();
        int num = (int) (timestamp % 4);
        account.setAvatarUrl("avatar/public/"+ num +".png");
        authRepository.save(account);

        // 创建 JWT
        //String token = jwtService.generateToken(account);
        Map<String, String> tokens = jwtService.generateTokens(account); // ⬅️ 在这里调用！
        authRepository.save(account);
        return Result.success(new LoginResponse(tokens));
    }

    // 登录
    public Result<?> login(LoginRequest request) {
        String identity = request.getIdentity();
        Role role = request.getRole();

        if (identity == null || identity.isBlank()) {
            throw new BizException("请输入邮箱或手机号");
        }
        boolean isEmail = isValidEmail(identity);
        boolean isPhone = isValidPhone(identity);
        if (!isEmail && !isPhone) {
            throw new BizException("账号必须是有效的邮箱或手机号");
        }

        Optional<Account> optionalAccount = isEmail
                ? authRepository.findByEmailAndRole(identity, role)
                : authRepository.findByPhoneAndRole(identity, role);

        if (optionalAccount.isEmpty()) {
            throw new BizException("账号不存在");
        }
        Account account = optionalAccount.get();

        if (account.getBanDurationDays() != 0) {
            if (account.getBanDurationDays() == -1)
                throw new BizException("账号已被永久封禁，如有疑问请联系管理员");
            LocalDateTime banEndTime = account.getBanStartTime().plusDays(account.getBanDurationDays());
            if (LocalDateTime.now().isAfter(banEndTime)) {
                // 解封时间已到，解除封禁
                account.setBanDurationDays(0);
                account.setBanStartTime(null);
                account.setBanReason(null);
                authRepository.save(account); // 保存变更
            } else {
                // 只显示到分钟（yyyy-MM-dd HH:mm）
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                String formattedBanEndTime = banEndTime.format(formatter);
                throw new BizException("账号暂时不可使用，被封禁至 " + formattedBanEndTime + "，如有疑问请联系管理员");
            }
        }
        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new BizException("密码错误");
        }

        // 创建 JWT
        //String token = jwtService.generateToken(account);
        Map<String, String> tokens = jwtService.generateTokens(account); // ⬅️ 在这里调用！
        account.setActive(true);
        authRepository.save(account);
        return Result.success(new LoginResponse(tokens));
    }

    // 登出
    public Result<?> logout(HttpServletRequest request) {
        Account account = accountUtil.getCurrentAccount();
        account.setActive(false);
        authRepository.save(account);
        //拉黑token
        String tokenHeader = request.getHeader("Authorization");
        String tokenPrefix = "Bearer ";
        if (StringUtils.hasText(tokenHeader) && tokenHeader.startsWith(tokenPrefix)) {
            String token = tokenHeader.substring(tokenPrefix.length());

            // 获取 token 剩余时间，设置 Redis 过期时间一致
            Date expiration = jwtService.extractExpiration(token);
            long ttl = expiration.getTime() - System.currentTimeMillis();

            if (ttl > 0) {
                redisTemplate.opsForValue().set("blacklist:" + token, "1", ttl, TimeUnit.MILLISECONDS);
            }
        }
        System.out.println("-------------------logout-------------------");
        return Result.success("");
    }

    //TODO:
    public Result<Map<String, String>> refreshAccessToken(String refreshToken) {
        // 1. 校验 refreshToken 存在性
        if (!StringUtils.hasText(refreshToken)) {
            throw new BizException("RefreshToken 缺失");
        }
        // 2. 解析 userId（即 JWT 中 subject）
        String userId;
        try {
            userId = jwtService.extractAccountId(refreshToken);
        } catch (Exception e) {
            throw new BizException("RefreshToken 非法");
        }

        // 3. 检查是否已过期
        if (jwtService.isTokenExpired(refreshToken)) {
            throw new BizException("RefreshToken 已过期");
        }

        //TODO
        // 4. 校验 Redis 中是否存在此 refreshToken
        String redisKey = "refreshlist:" + userId;
        List<String> tokenList = redisTemplate.opsForList().range(redisKey, 0, -1);
        if (!(tokenList != null && tokenList.contains(refreshToken)))
            throw new BizException("非法的 RefreshToken");
//        String storedToken = redisTemplate.opsForValue().get(redisKey);
//        System.out.println(storedToken);
//        if (storedToken == null || !storedToken.equals(refreshToken)) {
//            System.out.println("refreshToken:"+refreshToken);
//            System.out.println("storedToken:"+storedToken);
//            throw new BizException("非法的 RefreshToken");
//        }

        // 5. 生成新的 accessToken
        Account account = authRepository.findById(userId).orElseThrow(() -> new BizException("账户不存在"));
//        if(!userId.equals(AccountUtil.getCurrentAccountId()))
//            throw new BizException("非也");
        String newAccessToken = jwtService.generateToken(account);

        // （可选）refreshToken 快过期时，也重新颁发新的 refreshToken（建议提升安全性）
        // 此处我们不做更新，只返回新的 accessToken
        return Result.success(Map.of("accessToken", newAccessToken));
    }

    //重置密码
    public Result<?> resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail();
        String phone = request.getPhone();
        Role role = request.getRole();
        String code = request.getCode(); //验证码字段

        if ((email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
            throw new BizException("邮箱或手机号必须提供其中之一");
        }
        if (!isValidEmail(email) && !isValidPhone(phone)) {
            throw new BizException("账号必须是有效的邮箱或手机号");
        }

        Optional<Account> exists;
        if (email != null && !email.isBlank()) {
            if (!captchaService.verifyEmailCode(email, code))
                throw new BizException("邮箱验证码错误");
            exists = authRepository.findByEmailAndRole(email, role);
        } else {
            if (!captchaService.verifySmsCode(phone, code))
                throw new BizException("手机验证码错误");
            exists = authRepository.findByPhoneAndRole(phone, role);
        }
        if (exists.isEmpty()) {
            throw new BizException((email != null && !email.isBlank()) ? "该邮箱未被注册" : "该手机号未被注册");
        }
        exists.get().setPassword(passwordEncoder.encode(request.getNewpassword()));
        authRepository.save(exists.get());
        // 创建 JWT
        //String token = jwtService.generateToken(account);
        Account account = accountUtil.getCurrentAccount();
        Map<String, String> tokens = jwtService.generateTokens(account); // ⬅️ 在这里调用！
        authRepository.save(account);
        return Result.success(new LoginResponse(tokens));
    }

    public Result<?> updateUsername(UpdateUsernameRequest request) {
        Account account = accountUtil.getCurrentAccount();
        account.setUsername(request.getNewusername());
        authRepository.save(account);
        return Result.success("修改成功");
    }

    public Result<?> updateAvatar(MultipartFile avatarFile) {
        if (avatarFile.isEmpty())
            throw new BizException("文件不能为空");
        // 获取当前登录账号ID
        String accountId = AccountUtil.getCurrentAccountId();
        Account account = accountUtil.getCurrentAccount();
        // 检查文件类型
        String contentType = avatarFile.getContentType();
        if (contentType == null ||
                !(contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/gif"))) {
            throw new BizException("只支持 JPEG、PNG、GIF 格式的头像");
        }
        String objectkey = "avatar/" + accountId + "/" + avatarFile.getOriginalFilename();
        //String objectkey= "avatar/public/nailong.gif";
        try {
            AliyunOssUtil.delete(account.getAvatarUrl());
            AliyunOssUtil.upload(objectkey, avatarFile.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        account.setAvatarUrl(objectkey);
        // 调用 Service 完成保存头像操作
        authRepository.save(account);
        return Result.success("修改成功", AliyunOssUtil.generatePresignedGetUrl(account.getAvatarUrl(), 3600, IMAGE_PROCESS));
    }

    public Medto me() {
        Account account = accountUtil.getCurrentAccount();
        Medto dto = new Medto();
        dto.setId(account.getId());
        dto.setUsername(account.getUsername());
        dto.setEmail(account.getEmail());
        dto.setPhone(account.getPhone());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setUpdatedAt(account.getUpdatedAt());
        dto.setRole(account.getRole());
        dto.setAvatarUrl(AliyunOssUtil.generatePresignedGetUrl(account.getAvatarUrl(), 3600, IMAGE_PROCESS));
        return dto;
    }

    private boolean isValidEmail(String email) {
        return email != null && EmailValidator.getInstance().isValid(email);
    }

    private boolean isValidPhone(String phone) {
        // 中国大陆 11 位标准手机号
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }

    public Result<?> rebind(RebindRequest request) {
        String email = request.getNewEmail();
        String phone = request.getNewPhone();
        Role role = request.getRole();
        String code = request.getCode();

        if ((email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
            throw new BizException("邮箱或手机号必须提供其中之一");
        }

        Account account = accountUtil.getCurrentAccount();
        boolean exists;

        if (email != null && !email.isBlank()) {
            if (!isValidEmail(email)) throw new BizException("邮箱格式错误");
            if (!captchaService.verifyEmailCode(email, code)) throw new BizException("邮箱验证码错误");
            exists = authRepository.existsByEmailAndRoleAndIdNot(email, role, account.getId());
            if (exists) throw new BizException("该邮箱已被注册");
            account.setEmail(email);
        }

        if (phone != null && !phone.isBlank()) {
            if (!isValidPhone(phone)) throw new BizException("手机号格式错误");
            if (!captchaService.verifySmsCode(phone, code)) throw new BizException("手机验证码错误");
            exists = authRepository.existsByPhoneAndRoleAndIdNot(phone, role, account.getId());
            if (exists) throw new BizException("该手机号已被注册");
            account.setPhone(phone);
        }

        authRepository.save(account);
        return Result.success("修改成功");
    }
}
