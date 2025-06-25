package org.whu.backend.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.whu.backend.entity.accounts.Account;

public class SecurityUtil {
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
}
