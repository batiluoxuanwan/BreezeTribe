package org.whu.backend.repository.authRepo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.entity.accounts.Account;
import org.whu.backend.entity.accounts.Role;

import java.util.Optional;
import java.util.UUID;

/**
 * 定义好了标准的CRUD方法，包括：
 * save(T entity)
 * findById(ID id)
 * findAll()
 * delete(T entity) 等。
 */

public interface AuthRepository extends JpaRepository<Account, String> {
    boolean existsByEmailAndRole(String email, Role role);

    boolean existsByPhoneAndRole(String phone, Role role);

    Optional<Account> findByEmailAndRole(String email, Role role);

    Optional<Account> findByPhoneAndRole(String phone, Role role);

    Optional<Account> findByUsername(String username);
    Page<Account> findByRole(Role enumRole, Pageable pageable);

    Page<Account> findByUsernameContaining(String username, Pageable pageable);

    Page<Account> findByUsernameContainingAndRole(String username, Role role, Pageable pageable);

    boolean existsByEmailAndRoleAndIdNot(String email,Role role,String Id);
    boolean existsByPhoneAndRoleAndIdNot(String phone,Role role,String Id);
}

