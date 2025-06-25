package org.whu.backend.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.entity.accounts.Account;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.repository.authRepo.UserRepository;

import java.util.Optional;

@Component
public class SecurityUtil {
    @Autowired
    private UserRepository userRepository;

    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Account account)) {
            return null;
        }
        return account.getId();
    }
    /*
     * 在Service或Controller中调用
     * String userId = SecurityUtil.getCurrentUserId();
     * 获取用户ID
    */
    public User getCurrentUser() {
        if (SecurityUtil.getCurrentUserId() == null)
            throw new BizException("用户未登录");
        Optional<User> userOpt = userRepository.findById(SecurityUtil.getCurrentUserId());
        if (userOpt.isEmpty()) {
            throw new BizException("用户不存在");
        }
        return userOpt.get();
    }
}
