package com.insurai.controller;

import com.insurai.model.User;
import com.insurai.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepo;
    private final com.insurai.repository.CompanyRepository companyRepo;

    public UserController(UserRepository userRepo, com.insurai.repository.CompanyRepository companyRepo) {
        this.userRepo = userRepo;
        this.companyRepo = companyRepo;
    }

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPANY_ADMIN')")
    public java.util.List<User> getAllUsers() {
        // NOTE / SCOPE LEAK FLAG: Currently allows COMPANY_ADMIN to see all users system-wide.
        // In future phases, COMPANY_ADMIN scope should be scoped strictly to users of their own company.
        return userRepo.findAll();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProfile(
            @PathVariable Long id,
            @RequestBody User updates,
            @com.insurai.security.CurrentUser User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        // Ownership check: users can only update their own profile unless they hold SUPER_ADMIN or COMPANY_ADMIN roles.
        boolean isPrivileged = currentUser.getRole() != null && 
                ("SUPER_ADMIN".equalsIgnoreCase(currentUser.getRole()) || "COMPANY_ADMIN".equalsIgnoreCase(currentUser.getRole()));
        if (!currentUser.getId().equals(id) && !isPrivileged) {
            return ResponseEntity.status(403).body("Access denied: You can only update your own profile.");
        }

        return userRepo.findById(java.util.Objects.requireNonNull(id)).map(user -> {
            // Update only allowed fields
            if (updates.getName() != null)
                user.setName(updates.getName());
            if (updates.getPhone() != null)
                user.setPhone(updates.getPhone());
            if (updates.getAge() != null)
                user.setAge(updates.getAge());
            if (updates.getIncome() != null)
                user.setIncome(updates.getIncome());
            if (updates.getDependents() != null)
                user.setDependents(updates.getDependents());
            if (updates.getHealthInfo() != null)
                user.setHealthInfo(updates.getHealthInfo());

            // Agent specific
            if (updates.getSpecialization() != null)
                user.setSpecialization(updates.getSpecialization());
            if (updates.getBio() != null)
                user.setBio(updates.getBio());

            return ResponseEntity.ok(userRepo.save(java.util.Objects.requireNonNull(user)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/admin")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPANY_ADMIN')")
    public ResponseEntity<?> adminUpdateUser(@PathVariable Long id, @RequestBody User updates) {
        return userRepo.findById(java.util.Objects.requireNonNull(id)).map(user -> {
            if (updates.getName() != null)
                user.setName(updates.getName());
            if (updates.getEmail() != null)
                user.setEmail(updates.getEmail());
            if (updates.getRole() != null)
                user.setRole(updates.getRole());
            if (updates.getIsActive() != null)
                user.setIsActive(updates.getIsActive());

            // Assign company for Agents
            if ("AGENT".equalsIgnoreCase(user.getRole())) {
                if (updates.getMappingCompanyId() != null) {
                    Long cId = updates.getMappingCompanyId();
                    if (cId != null) {
                        companyRepo.findById(java.util.Objects.requireNonNull(cId)).ifPresent(user::setCompany);
                    }
                } else if (updates.getCompany() != null) {
                    // This covers cases where nested JSON is sent
                    user.setCompany(updates.getCompany());
                }
            }

            // Also allow updating standard fields
            if (updates.getPhone() != null)
                user.setPhone(updates.getPhone());
            // ... can add others if needed

            return ResponseEntity.ok(userRepo.save(java.util.Objects.requireNonNull(user)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProfile(@PathVariable Long id) {
        return userRepo.findById(java.util.Objects.requireNonNull(id))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
