package org.whu.backend.repository.authRepo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.entity.accounts.Merchant;

import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, String> {
    //    // 获取所有审核已通过的商家
//    List<Merchant> findByApprovedTrue();
//    // 获取所有待审核的商家
//    List<Merchant> findByApprovedFalse();
    // 按邮箱获取某商家
    Optional<Merchant> findByEmail(String email);
    // 按手机号获取某商家
    Optional<Merchant> findByPhone(String phone);
    // 按id获取
    //Optional<Merchant> findById(String id);
}