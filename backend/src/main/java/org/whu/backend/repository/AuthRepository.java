package org.whu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.entity.Accounts.Account;
import org.whu.backend.entity.Accounts.Role;

import java.util.Optional;
import java.util.UUID;

public interface AuthRepository extends JpaRepository<Account, UUID> {
    boolean existsByEmailAndRole(String email, Role role);
    boolean existsByPhoneAndRole(String phone, Role role);

    Optional<Account> findByEmailAndRole(String email, Role role);
    Optional<Account> findByPhoneAndRole(String phone, Role role);
}

