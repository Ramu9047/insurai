package com.insurai.controller;

import com.insurai.model.Feedback;
import com.insurai.model.User;

import com.insurai.repository.FeedbackRepository;
import com.insurai.repository.UserRepository;
import com.insurai.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final UserRepository userRepo;
    private final com.insurai.repository.CompanyRepository companyRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    public FeedbackController(FeedbackService feedbackService, UserRepository userRepo,
            com.insurai.repository.CompanyRepository companyRepository) {
        this.feedbackService = feedbackService;
        this.userRepo = userRepo;
        this.companyRepository = companyRepository;
    }

    // Submit Feedback
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('AGENT') or hasRole('COMPANY')")
    public ResponseEntity<Feedback> submitFeedback(@RequestBody Map<String, String> body, Authentication auth) {
        String email = auth.getName();
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String category = body.get("category");
        String subject = body.get("subject");
        String description = body.get("description");

        Feedback feedback = feedbackService.submitFeedback(user.getId(), category, subject, description);
        return ResponseEntity.ok(feedback);
    }

    // Get My Feedback
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER') or hasRole('AGENT') or hasRole('COMPANY')")
    public List<Feedback> getMyFeedback(Authentication auth) {
        String email = auth.getName();
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return feedbackService.getUserFeedback(user.getId());
    }

    // Admin: Get All Feedback (Super Admin sees ALL, Company Admin sees scoped)
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPANY_ADMIN', 'COMPANY')")
    public ResponseEntity<?> getAllFeedback(Authentication auth) {
        String email = auth.getName();
        Long companyId = null;

        var companyOpt = companyRepository.findByEmail(email);
        if (companyOpt.isPresent()) {
            companyId = companyOpt.get().getId();
        } else {
            User currentUser = userRepo.findByEmail(email).orElse(null);
            if (currentUser != null
                    && ("COMPANY_ADMIN".equals(currentUser.getRole()) || "COMPANY".equals(currentUser.getRole()))
                    && currentUser.getCompany() != null) {
                companyId = currentUser.getCompany().getId();
            }
        }

        if (companyId != null) {
            // Company Admin: return only feedback from users of their company
            final Long resolvedCompanyId = companyId;
            List<Feedback> scoped = feedbackRepository.findAll().stream()
                    .filter(f -> f.getUser() != null && f.getUser().getCompany() != null
                            && resolvedCompanyId.equals(f.getUser().getCompany().getId()))
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(scoped);
        }
        return ResponseEntity.ok(feedbackService.getAllFeedback());
    }

    // Admin: Update Status
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPANY_ADMIN')")
    public Feedback updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        String adminResponse = body.get("adminResponse");
        return feedbackService.updateStatus(id, status, adminResponse);
    }

    // Admin: Assign Feedback
    @PatchMapping("/{id}/assign/{assigneeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPANY_ADMIN')")
    public Feedback assignFeedback(@PathVariable Long id, @PathVariable Long assigneeId) {
        return feedbackService.assignFeedback(id, assigneeId);
    }
}
