package org.whu.backend.service.auth;

import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.Optional;
import java.util.Random;

import static org.whu.backend.entity.accounts.Merchant.status.PENDING;

@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final CaptchaService captchaService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    @Autowired
    private AccountUtil accountUtil;

    public AuthService(AuthRepository authRepository,
                       CaptchaService captchaService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.authRepository = authRepository;
        this.captchaService = captchaService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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
            if(!captchaService.verifyCode(email, code))
                throw new BizException("邮箱验证码错误");
            exists = authRepository.existsByEmailAndRole(email, role);
        } else {
            if(!captchaService.verifyCode(phone, code))
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
                user.setUsername("新用户"+generatename());
                //user.setEnabled(true);
                user.setBanDurationDays(0);
                account = user;
                break;
            case ROLE_MERCHANT:
                Merchant merchant = new Merchant();
                merchant.setRole(Role.ROLE_MERCHANT);
                merchant.setUsername("新商家"+generatename());
                //merchant.setEnabled(false);
                merchant.setBanDurationDays(-1);
                merchant.setApproval(PENDING);
                // 特有字段暂时不填，后续用接口补充
                account = merchant;
                break;
            case ROLE_ADMIN:
                Admin admin = new Admin();
                admin.setRole(Role.ROLE_ADMIN);
                admin.setUsername("管理员"+generatename());
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
        account.setAvatarUrl("avatar/public/nailong.gif");
        authRepository.save(account);

        String token = jwtService.generateToken(account);
        return Result.success(new LoginResponse(token));
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
        if (account.getBanDurationDays()!=0) {
            //throw new BizException("账号暂时不可使用，请联系管理员");
            LocalDateTime banEndTime = account.getBanStartTime().plusDays(account.getBanDurationDays());
            throw new BizException("账号暂时不可使用，被封禁至"+banEndTime+"如有疑问请联系管理员");
        }
        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new BizException("密码错误");
        }

        // 创建 JWT
        String token = jwtService.generateToken(account);
        return Result.success(new LoginResponse(token));
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

        Optional<Account> exists ;
        if (email != null && !email.isBlank()) {
            if(!captchaService.verifyCode(email, code))
                throw new BizException("邮箱验证码错误");
            exists = authRepository.findByEmailAndRole(email, role);
        } else {
            if(!captchaService.verifyCode(phone, code))
                throw new BizException("手机验证码错误");
            exists = authRepository.findByPhoneAndRole(phone, role);
        }
        if (exists.isEmpty()) {
            throw new BizException((email != null && !email.isBlank()) ? "该邮箱未被注册" : "该手机号未被注册");
        }
        exists.get().setPassword(passwordEncoder.encode(request.getNewpassword()));
        authRepository.save(exists.get());
        String token = jwtService.generateToken(exists.get());
        return Result.success(new LoginResponse(token));
    }
    public Result<?> updateUsername(UpdateUsernameRequest request)  {
        Account account =accountUtil.getCurrentAccount();
        account.setUsername(request.getNewusername());
        authRepository.save(account);
        return Result.success("修改成功");
    }
    public Result<?> updateAvatar(MultipartFile avatarFile)
    {
        if (avatarFile.isEmpty())
            throw new BizException("文件不能为空");
        // 获取当前登录账号ID
        String accountId = AccountUtil.getCurrentAccountId();
        Account account= accountUtil.getCurrentAccount();
        // 检查文件类型
        String contentType = avatarFile.getContentType();
        if (contentType == null ||
                !(contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/gif"))) {
            throw new BizException("只支持 JPEG、PNG、GIF 格式的头像");
        }
        String objectkey= "avatar/"+accountId+"/"+avatarFile.getOriginalFilename();
        //String objectkey= "avatar/public/nailong.gif";
        try {
            AliyunOssUtil.delete(account.getAvatarUrl());
            AliyunOssUtil.upload(objectkey,avatarFile.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        account.setAvatarUrl(objectkey);
        // 调用 Service 完成保存头像操作
        authRepository.save(account);
        return Result.success("修改成功");
    }
    public Medto me()
    {
        Account account= accountUtil.getCurrentAccount();
        Medto dto=new Medto();
        dto.setId(account.getId());
        dto.setUsername(account.getUsername());
        dto.setEmail(account.getEmail());
        dto.setPhone(account.getPhone());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setUpdatedAt(account.getUpdatedAt());
        dto.setRole(account.getRole());
        dto.setAvatarUrl(AliyunOssUtil.generatePresignedGetUrl(account.getAvatarUrl(), 3600));
        return dto;
    }

    private boolean isValidEmail(String email) {
        return email != null && EmailValidator.getInstance().isValid(email);
    }

    private boolean isValidPhone(String phone) {
        // 中国大陆 11 位标准手机号
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }

}
