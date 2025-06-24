package org.whu.backend.service;

import org.springframework.stereotype.Service;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.whu.backend.common.Result;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.auth.LoginRequest;
import org.whu.backend.dto.auth.LoginResponse;
import org.whu.backend.dto.auth.RegisterRequest;
import org.whu.backend.entity.Account;
import org.whu.backend.entity.Role;
import org.whu.backend.repository.AuthRepository;

import java.util.Optional;

@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final CaptchaService captchaService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AuthRepository authRepository,
                       CaptchaService captchaService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.authRepository = authRepository;
        this.captchaService = captchaService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // 注册
    public Result register(RegisterRequest request) {
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

        Account account = new Account();
        account.setEmail(email);
        account.setPhone(phone);
        account.setRole(role);
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        authRepository.save(account);

        String token = jwtService.generateToken(account);
        return Result.success(new LoginResponse(token));
    }

    // 登录
    public Result login(LoginRequest request) {
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

        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new BizException("密码错误");
        }

        // 创建 JWT
        String token = jwtService.generateToken(account);
        return Result.success(new LoginResponse(token));
    }


    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^\\d{10,15}$"); // 可根据实际手机号位数修改
    }
}
