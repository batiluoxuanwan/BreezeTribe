package org.whu.backend.util;

import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.common.exception.BizException;

public class JpaUtil {

    // 从数据库中按id获取对象，如果不存在则抛出异常
    public static <T, ID> T getOrThrow(JpaRepository<T, ID> repository, ID id, String errorMessage) {
        return repository.findById(id).orElseThrow(() -> new BizException(errorMessage));
    }
    /*
    使用
    TravelPackage travelPackage = JpaUtil.getOrThrow(travelPackageRepository, orderCreateRequestDto.getPackageId(), "旅行团不存在");
     */

    public static <T, ID> boolean ensureNotExists(JpaRepository<T, ID> repository, ID id, String errorMessage) {
        if (repository.findById(id).isPresent()) {
            throw new BizException(errorMessage);
        }
        return true;
    }
    /*
    使用
    JpaUtil.ensureNotExists(userRepository, someUserId, "用户已经存在");
     */
}