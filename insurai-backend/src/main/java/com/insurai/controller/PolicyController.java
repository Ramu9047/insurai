package com.insurai.controller;

import com.insurai.model.Policy;
import com.insurai.model.User;
import com.insurai.model.UserPolicy;
import com.insurai.repository.UserPolicyRepository;
import com.insurai.repository.UserRepository;
import com.insurai.security.CurrentUser;
import com.insurai.service.FileStorageService;
import com.insurai.service.PolicyService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final UserRepository userRepo;
    private final UserPolicyRepository userPolicyRepo;
    private final PolicyService policyService;
    private final FileStorageService fileStorageService;

    public PolicyController(
            PolicyService policyService,
            UserRepository userRepo,
            UserPolicyRepository userPolicyRepo,
            FileStorageService fileStorageService) {
        this.policyService = policyService;
        this.userRepo = userRepo;
        this.userPolicyRepo = userPolicyRepo;
        this.fileStorageService = fileStorageService;
    }

    private User getCurrentUser() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        String email = (String) auth.getPrincipal();
        return userRepo.findByEmail(email).orElse(null);
    }

    @GetMapping
    public List<Policy> getAll() {
        User user = getCurrentUser();
        String role = (user != null) ? user.getRole() : "USER";
        long userId = (user != null && user.getId() != null) ? user.getId() : 0L;
        return policyService.getAll(role, userId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY', 'SUPER_ADMIN')")
    public Policy create(@RequestBody Policy policy) {
        return policyService.create(java.util.Objects.requireNonNull(policy), getCurrentUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY', 'COMPANY_ADMIN', 'SUPER_ADMIN')")
    public Policy update(@PathVariable long id, @RequestBody Policy policy) {
        return policyService.update(id, java.util.Objects.requireNonNull(policy), getCurrentUser());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY', 'COMPANY_ADMIN', 'SUPER_ADMIN')")
    public void delete(@PathVariable long id) {
        policyService.delete(id, getCurrentUser());
    }

    @PostMapping("/{policyId}/buy/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'COMPANY_ADMIN', 'SUPER_ADMIN')")
    public UserPolicy buyPolicy(
            @PathVariable long policyId,
            @PathVariable long userId,
            @CurrentUser User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        // Ownership check: Standard users can only buy policies for themselves. SUPER_ADMIN and COMPANY_ADMIN roles are privileged overrides.
        boolean isPrivileged = currentUser.getRole() != null &&
                ("SUPER_ADMIN".equalsIgnoreCase(currentUser.getRole()) || "COMPANY_ADMIN".equalsIgnoreCase(currentUser.getRole()));
        if (!currentUser.getId().equals(userId) && !isPrivileged) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Cannot buy policy for another user");
        }

        return policyService.buyPolicy(policyId, userId);
    }

    @PostMapping("/{policyId}/quote/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'COMPANY_ADMIN', 'SUPER_ADMIN')")
    public UserPolicy quotePolicy(
            @PathVariable long policyId,
            @PathVariable long userId,
            @jakarta.validation.Valid @RequestBody(required = false) com.insurai.dto.QuoteRequest request,
            @CurrentUser User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        // Ownership check: Standard users can only request policy quotes for themselves. SUPER_ADMIN and COMPANY_ADMIN roles are privileged overrides.
        boolean isPrivileged = currentUser.getRole() != null &&
                ("SUPER_ADMIN".equalsIgnoreCase(currentUser.getRole()) || "COMPANY_ADMIN".equalsIgnoreCase(currentUser.getRole()));
        if (!currentUser.getId().equals(userId) && !isPrivileged) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Cannot request quote for another user");
        }

        String note = (request != null) ? request.getNote() : null;
        return policyService.quotePolicy(policyId, userId, note);
    }

    @PostMapping("/{userPolicyId}/purchase")
    @PreAuthorize("hasAnyRole('USER', 'COMPANY_ADMIN', 'SUPER_ADMIN')")
    public UserPolicy purchasePolicy(
            @PathVariable long userPolicyId,
            @CurrentUser User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        UserPolicy userPolicy = userPolicyRepo.findById(userPolicyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User policy not found"));

        // Ownership check: Users can only purchase/activate their own issued user policy. SUPER_ADMIN and COMPANY_ADMIN roles are privileged overrides.
        boolean isPrivileged = currentUser.getRole() != null &&
                ("SUPER_ADMIN".equalsIgnoreCase(currentUser.getRole()) || "COMPANY_ADMIN".equalsIgnoreCase(currentUser.getRole()));
        if (!isPrivileged && (userPolicy.getUser() == null || !currentUser.getId().equals(userPolicy.getUser().getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Cannot purchase policy for another user");
        }

        return policyService.purchasePolicy(userPolicyId);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'COMPANY_ADMIN', 'SUPER_ADMIN')")
    public List<UserPolicy> getUserPolicies(
            @PathVariable long userId,
            @CurrentUser User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        // Ownership check: Users can only view their own user policies. AGENT, SUPER_ADMIN, and COMPANY_ADMIN roles are privileged overrides.
        boolean isPrivileged = currentUser.getRole() != null &&
                ("SUPER_ADMIN".equalsIgnoreCase(currentUser.getRole()) || "COMPANY_ADMIN".equalsIgnoreCase(currentUser.getRole()) || "AGENT".equalsIgnoreCase(currentUser.getRole()));
        if (!currentUser.getId().equals(userId) && !isPrivileged) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Cannot view policies of another user");
        }

        return policyService.getUserPolicies(userId);
    }

    @PostMapping("/upload/{userPolicyId}")
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'COMPANY_ADMIN', 'SUPER_ADMIN')")
    public UserPolicy uploadDocument(
            @PathVariable Long userPolicyId,
            @RequestParam("file") MultipartFile file,
            @CurrentUser User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        UserPolicy userPolicy = userPolicyRepo.findById(java.util.Objects.requireNonNull(userPolicyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User policy not found"));

        // Ownership check: Users can only upload documents to their own user policy. AGENT, SUPER_ADMIN, and COMPANY_ADMIN roles are privileged overrides.
        boolean isPrivileged = currentUser.getRole() != null &&
                ("SUPER_ADMIN".equalsIgnoreCase(currentUser.getRole()) || "COMPANY_ADMIN".equalsIgnoreCase(currentUser.getRole()) || "AGENT".equalsIgnoreCase(currentUser.getRole()));
        if (!isPrivileged && (userPolicy.getUser() == null || !currentUser.getId().equals(userPolicy.getUser().getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Cannot upload document to another user's policy");
        }

        String fileUrl = fileStorageService.storeFile(file);
        return policyService.uploadDocument(userPolicyId, fileUrl);
    }

    @GetMapping("/recommendations/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'COMPANY_ADMIN', 'SUPER_ADMIN')")
    public List<com.insurai.dto.PolicyRecommendationDTO> getRecommendations(
            @PathVariable long userId,
            @CurrentUser User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        // Ownership check: Users can only view their own policy recommendations. AGENT, SUPER_ADMIN, and COMPANY_ADMIN roles are privileged overrides.
        boolean isPrivileged = currentUser.getRole() != null &&
                ("SUPER_ADMIN".equalsIgnoreCase(currentUser.getRole()) || "COMPANY_ADMIN".equalsIgnoreCase(currentUser.getRole()) || "AGENT".equalsIgnoreCase(currentUser.getRole()));
        if (!currentUser.getId().equals(userId) && !isPrivileged) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Cannot view recommendations for another user");
        }

        return policyService.getRecommendedPolicies(userId);
    }

    @GetMapping("/issued")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPANY_ADMIN', 'COMPANY')")
    public List<UserPolicy> getAllUserPolicies(org.springframework.security.core.Authentication auth) {
        return policyService.getAllUserPolicies(auth);
    }

    @PostMapping("/filter/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'COMPANY_ADMIN', 'SUPER_ADMIN')")
    public List<com.insurai.dto.PolicyRecommendationDTO> filterPolicies(
            @PathVariable long userId,
            @jakarta.validation.Valid @RequestBody com.insurai.dto.PolicyFilterRequest filter,
            @CurrentUser User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        // Ownership check: Users can only filter policy recommendations for themselves. AGENT, SUPER_ADMIN, and COMPANY_ADMIN roles are privileged overrides.
        boolean isPrivileged = currentUser.getRole() != null &&
                ("SUPER_ADMIN".equalsIgnoreCase(currentUser.getRole()) || "COMPANY_ADMIN".equalsIgnoreCase(currentUser.getRole()) || "AGENT".equalsIgnoreCase(currentUser.getRole()));
        if (!currentUser.getId().equals(userId) && !isPrivileged) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Cannot filter policies for another user");
        }

        return policyService.getFilteredPolicies(userId, filter);
    }
}
