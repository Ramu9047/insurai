package com.insurai.controller;

import com.insurai.model.Claim;
import com.insurai.service.ClaimService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
@CrossOrigin(origins = "http://localhost:3000")
public class ClaimController {

    private final ClaimService claimService;
    private final com.insurai.repository.UserRepository userRepo;

    public ClaimController(ClaimService claimService, com.insurai.repository.UserRepository userRepo) {
        this.claimService = claimService;
        this.userRepo = userRepo;
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

        try {
            if (file.isEmpty())
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Empty file");

            String rawName = file.getOriginalFilename();
            if (rawName == null || rawName.trim().isEmpty()) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid filename");
            }

            String safeName = java.nio.file.Paths.get(rawName).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
            String ext = "";
            int dotIdx = safeName.lastIndexOf('.');
            if (dotIdx > 0) {
                ext = safeName.substring(dotIdx + 1).toLowerCase();
            }

            List<String> allowedExtensions = List.of("pdf", "jpg", "jpeg", "png", "doc", "docx");
            if (!allowedExtensions.contains(ext)) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Unsupported file format. Allowed: pdf, jpg, jpeg, png, doc, docx");
            }

            String fileName = System.currentTimeMillis() + "_" + safeName;
            java.nio.file.Path uploadsDir = java.nio.file.Paths.get("uploads").toAbsolutePath().normalize();
            java.nio.file.Path path = uploadsDir.resolve(fileName).normalize();
            if (!path.startsWith(uploadsDir)) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid upload path");
            }

            java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.copy(file.getInputStream(), path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "http://localhost:8080/uploads/" + fileName;
            return claimService.uploadDoc(java.util.Objects.requireNonNull(id), fileUrl);
        } catch (java.io.IOException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload claim document", e);
        }
    }
}
