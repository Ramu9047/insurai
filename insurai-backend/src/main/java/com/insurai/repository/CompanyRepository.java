package com.insurai.repository;

import com.insurai.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByEmail(String email);

    List<Company> findByStatus(String status);

    List<Company> findByIsActive(Boolean isActive);

    List<Company> findByStatusAndIsActive(String status, Boolean isActive);

    boolean existsByEmail(String email);

    boolean existsByRegistrationNumber(String registrationNumber);

    Optional<Company> findFirstByName(String name);

    Optional<Company> findFirstByNameIgnoreCase(String name);

    Optional<Company> findFirstByEmailIgnoreCase(String email);
}
