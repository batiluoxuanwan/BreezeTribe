package org.whu.backend.repository.authRepo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.entity.accounts.Admin;

import java.util.Optional;
import java.util.UUID;

public interface AdminRepository extends JpaRepository<Admin, UUID> {
    // 按邮箱获取某商家
    Optional<Admin> findByEmail(String email);

    // 按手机号获取某商家
    Optional<Admin> findByPhone(String phone);
}
