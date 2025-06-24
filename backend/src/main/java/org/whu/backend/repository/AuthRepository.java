package org.whu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.entity.Accounts.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 定义好了标准的CRUD方法，包括：
 * save(T entity)
 * findById(ID id)
 * findAll()
 * delete(T entity) 等。
*/

public interface AuthRepository extends JpaRepository<Account, UUID> {
    boolean existsByEmailAndRole(String email, Role role);
    boolean existsByPhoneAndRole(String phone, Role role);

    Optional<Account> findByEmailAndRole(String email, Role role);
    Optional<Account> findByPhoneAndRole(String phone, Role role);
}
interface UserRepository extends JpaRepository<User, UUID> {
    // 按邮箱获取某商家
    Optional<User> findByEmail(String email);
    // 按手机号获取某商家
    Optional<User> findByPhone(String phone);
}
interface MerchantRepository extends JpaRepository<Merchant, UUID> {
    // 获取所有审核已通过的商家
    List<Merchant> findByApprovedTrue();
    // 获取所有待审核的商家
    List<Merchant> findByApprovedFalse();
    // 按邮箱获取某商家
    Optional<Merchant> findByEmail(String email);
    // 按手机号获取某商家
    Optional<Merchant> findByPhone(String phone);
}
interface AdminRepository extends JpaRepository<Admin, UUID> {
    // 按邮箱获取某商家
    Optional<Admin> findByEmail(String email);
    // 按手机号获取某商家
    Optional<Admin> findByPhone(String phone);
}