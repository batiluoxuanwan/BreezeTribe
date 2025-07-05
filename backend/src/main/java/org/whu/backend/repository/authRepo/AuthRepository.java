package org.whu.backend.repository.authRepo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.whu.backend.entity.accounts.Account;
import org.whu.backend.entity.accounts.Role;
import org.whu.backend.repository.projection.TimeCount;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    Optional<Account> findByPhone(String phone);

    Optional<Account> findByEmail(String email);

    List<Account> findByUsernameContaining(String username); // 模糊查询

    @Query("""
        SELECT FUNCTION('DATE', a.createdAt), COUNT(a)
        FROM Account a
        WHERE a.createdAt BETWEEN :start AND :end
        AND (:role IS NULL OR a.role = :role)
        GROUP BY FUNCTION('DATE', a.createdAt)
        ORDER BY FUNCTION('DATE', a.createdAt)
        """)
    List<Object[]> countByDay(@Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end,
                              @Param("role") Role role);

    @Query("""
        SELECT FUNCTION('YEARWEEK', a.createdAt), COUNT(a)
        FROM Account a
        WHERE a.createdAt BETWEEN :start AND :end
        AND (:role IS NULL OR a.role = :role)
        GROUP BY FUNCTION('YEARWEEK', a.createdAt)
        ORDER BY FUNCTION('YEARWEEK', a.createdAt)
        """)
    List<Object[]> countByWeek(@Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end,
                               @Param("role") Role role);

    @Query("""
        SELECT FUNCTION('DATE_FORMAT', a.createdAt, '%Y-%m'), COUNT(a)
        FROM Account a
        WHERE a.createdAt BETWEEN :start AND :end
        AND (:role IS NULL OR a.role = :role)
        GROUP BY FUNCTION('DATE_FORMAT', a.createdAt, '%Y-%m')
        ORDER BY FUNCTION('DATE_FORMAT', a.createdAt, '%Y-%m')
        """)
    List<Object[]> countByMonth(@Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end,
                                @Param("role") Role role);
}

