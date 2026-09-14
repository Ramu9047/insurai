package com.insurai.repository;

import com.insurai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByResetToken(String resetToken);

    List<User> findByRole(String role);

    List<User> findByRoleAndIsActive(String role, Boolean isActive);

    List<User> findByRoleAndAvailable(String role, Boolean available);

    long countByRole(String role);

    long countByCompanyId(Long companyId);

    long countByRoleAndCompanyId(String role, Long companyId);

    List<User> findByCompanyId(Long companyId);

    List<User> findByCompanyIdAndRole(Long companyId, String role);

    long countByCompanyIdAndRole(Long companyId, String role);
}
