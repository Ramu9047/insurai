package com.insurai.controller;

import com.insurai.model.Document;
import com.insurai.model.User;
import com.insurai.repository.DocumentRepository;
import com.insurai.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class DocumentController {

    private final DocumentRepository documentRepo;
    private final UserRepository userRepo;

    public DocumentController(DocumentRepository documentRepo, UserRepository userRepo) {
        this.documentRepo = documentRepo;
        this.userRepo = userRepo;
    }

    // --- User Endpoints ---

    @GetMapping("/users/{userId}/documents")
    public List<Document> getUserDocuments(
            @PathVariable Long userId,
            @com.insurai.security.CurrentUser User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        boolean isPrivileged = "AGENT".equals(currentUser.getRole()) || "COMPANY_ADMIN".equals(currentUser.getRole())
                || "SUPER_ADMIN".equals(currentUser.getRole());
        if (!isPrivileged && !currentUser.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Cannot view documents of another user");
        }
        return documentRepo.findByUserId(userId);
    }

    @PostMapping("/users/{userId}/documents")
    public Document uploadDocument(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @com.insurai.security.CurrentUser User currentUser) throws IOException {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        boolean isPrivileged = "AGENT".equals(currentUser.getRole()) || "COMPANY_ADMIN".equals(currentUser.getRole())
                || "SUPER_ADMIN".equals(currentUser.getRole());
        if (!isPrivileged && !currentUser.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Cannot upload document for another user");
        }

        User user = userRepo.findById(java.util.Objects.requireNonNull(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (file.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file");

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
        }

        // Sanitize filename to prevent path traversal
        String safeName = Paths.get(originalName).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
        String ext = "";
        int dotIdx = safeName.lastIndexOf('.');
        if (dotIdx > 0) {
            ext = safeName.substring(dotIdx + 1).toLowerCase();
        }

        List<String> allowedExtensions = List.of("pdf", "jpg", "jpeg", "png", "doc", "docx");
        if (!allowedExtensions.contains(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported file format. Allowed: pdf, jpg, jpeg, png, doc, docx");
        }

        String fileName = System.currentTimeMillis() + "_" + safeName;
        Path uploadsDir = Paths.get("uploads").toAbsolutePath().normalize();
        Path path = uploadsDir.resolve(fileName).normalize();
        if (!path.startsWith(uploadsDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid upload path");
        }
        Files.createDirectories(path.getParent());
        Files.copy(file.getInputStream(), path);

        Document doc = new Document();
        doc.setUser(user);
        doc.setName(type); // Using type as name for now
        doc.setFilename(file.getOriginalFilename());
        doc.setType(type);
        doc.setStatus("PENDING");
        doc.setUrl("http://localhost:8080/uploads/" + fileName);
        doc.setSize(file.getSize());

        return documentRepo.save(doc);
    }

    // --- Agent/Admin Endpoints ---

    @PatchMapping("/documents/{docId}/verify")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN') or hasRole('COMPANY_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> verifyDocument(@PathVariable Long docId, Authentication auth) {
        Document doc = documentRepo.findById(java.util.Objects.requireNonNull(docId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        String verifierName = auth != null ? auth.getName() : "Unknown Agent"; // In real app, fetch User name

        doc.setStatus("VERIFIED");
        doc.setVerifiedBy(verifierName);
        doc.setVerifiedAt(LocalDateTime.now());
        documentRepo.save(doc);

        return ResponseEntity.ok(Map.of("message", "Document verified"));
    }

    @PatchMapping("/documents/{docId}/reject")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN') or hasRole('COMPANY_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> rejectDocument(@PathVariable Long docId, @RequestBody Map<String, String> body) {
        Document doc = documentRepo.findById(java.util.Objects.requireNonNull(docId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        String reason = body.get("reason");
        doc.setStatus("REJECTED");
        doc.setRejectionReason(reason);
        documentRepo.save(doc);

        return ResponseEntity.ok(Map.of("message", "Document rejected"));
    }

    @DeleteMapping("/documents/{docId}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable Long docId,
            @com.insurai.security.CurrentUser User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        Document doc = documentRepo.findById(java.util.Objects.requireNonNull(docId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        boolean isPrivileged = "AGENT".equals(currentUser.getRole()) || "COMPANY_ADMIN".equals(currentUser.getRole())
                || "SUPER_ADMIN".equals(currentUser.getRole());
        if (!isPrivileged && (doc.getUser() == null || !currentUser.getId().equals(doc.getUser().getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Cannot delete another user's document");
        }

        documentRepo.deleteById(docId);
        return ResponseEntity.ok(Map.of("message", "Document deleted"));
    }

    @GetMapping("/documents/pending")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    public List<Document> getPendingDocuments() {
        return documentRepo.findByStatus("PENDING");
    }
}
