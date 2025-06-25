package org.whu.backend.repository.authRepo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.entity.accounts.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    // 按邮箱获取某商家
    Optional<User> findByEmail(String email);

    // 按手机号获取某商家
    Optional<User> findByPhone(String phone);
}
