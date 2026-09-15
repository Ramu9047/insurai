package com.insurai.controller;

import com.insurai.model.Claim;
import com.insurai.service.ClaimService;
import com.insurai.service.FileStorageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;
    private final com.insurai.repository.UserRepository userRepo;
    private final FileStorageService fileStorageService;

    public ClaimController(
            ClaimService claimService,
            com.insurai.repository.UserRepository userRepo,
            FileStorageService fileStorageService) {
        this.claimService = claimService;
        this.userRepo = userRepo;
        this.fileStorageService = fileStorageService;
    }

    // User: File a claim
    @PostMapping("/{userId}")
    public Claim fileClaim(
            @PathVariable Long userId,
            @RequestBody Claim claim,
            @com.insurai.security.CurrentUser com.insurai.model.User currentUser) {
        if (currentUser == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        boolean isPrivileged = "AGENT".equals(currentUser.getRole()) || "COMPANY_ADMIN".equals(currentUser.getRole())
                || "SUPER_ADMIN".equals(currentUser.getRole());
        if (!isPrivileged && !currentUser.getId().equals(userId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Access denied: Cannot file claim for another user");
        }
        return claimService.fileClaim(java.util.Objects.requireNonNull(userId), claim);
    }

    // User: Get my claims
    @GetMapping("/user/{userId}")
    public List<Claim> getUserClaims(
            @PathVariable Long userId,
            @com.insurai.security.CurrentUser com.insurai.model.User currentUser) {
        if (currentUser == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        boolean isPrivileged = "AGENT".equals(currentUser.getRole()) || "COMPANY_ADMIN".equals(currentUser.getRole())
                || "SUPER_ADMIN".equals(currentUser.getRole());
        if (!isPrivileged && !currentUser.getId().equals(userId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Access denied: Cannot view claims of another user");
        }
        return claimService.getUserClaims(java.util.Objects.requireNonNull(userId));
    }

    // Admin/Agent: Get all claims
    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('AGENT','COMPANY','COMPANY_ADMIN','SUPER_ADMIN')")
    public List<Claim> getAllClaims(org.springframework.security.core.Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            String email = auth.getName();
            com.insurai.model.User user = userRepo.findByEmail(email).orElse(null);
            if (user != null) {
                if ("AGENT".equals(user.getRole()) && user.getCompany() != null) {
                    return claimService.getClaimsByCompany(user.getCompany().getId());
                }
                if (("COMPANY".equals(user.getRole()) || "COMPANY_ADMIN".equals(user.getRole()))
                        && user.getCompany() != null) {
                    return claimService.getClaimsByCompany(user.getCompany().getId());
                }
            }
        }
        return claimService.getAllClaims();
    }

    // Admin/Agent: Update status
    @PutMapping("/{id}/status")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('AGENT','COMPANY','COMPANY_ADMIN','SUPER_ADMIN')")
    public Claim updateStatus(@PathVariable Long id, @RequestParam String status) {
        return claimService.updateStatus(java.util.Objects.requireNonNull(id), status);
    }

    // User: Upload document to claim
    @PostMapping("/{id}/upload")
    public Claim uploadDoc(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @com.insurai.security.CurrentUser com.insurai.model.User currentUser) {
        if (currentUser == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        Claim claim = claimService.getAllClaims().stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Claim not found"));

        boolean isPrivileged = "AGENT".equals(currentUser.getRole()) || "COMPANY_ADMIN".equals(currentUser.getRole())
                || "SUPER_ADMIN".equals(currentUser.getRole());
        if (!isPrivileged && (claim.getUser() == null || !currentUser.getId().equals(claim.getUser().getId()))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Access denied: Cannot upload doc to another user's claim");
        }

        String fileUrl = fileStorageService.storeFile(file);
        return claimService.uploadDoc(java.util.Objects.requireNonNull(id), fileUrl);
    }
}
