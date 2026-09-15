package com.insurai.controller;

import com.insurai.model.SmartReminder;
import com.insurai.service.SmartReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Smart Reminder Controller
 * Provides endpoints for managing intelligent reminders
 */
@RestController
@RequestMapping("/api/reminders")
public class SmartReminderController {

    @Autowired
    private com.insurai.repository.SmartReminderRepository smartReminderRepository;

    @Autowired
    private SmartReminderService smartReminderService;

    /**
     * Get pending reminders for current user (privileged roles may pass optional userId to query on behalf of others).
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'SUPER_ADMIN', 'COMPANY_ADMIN')")
    public ResponseEntity<?> getPendingReminders(
            @RequestParam(required = false) Long userId,
            @com.insurai.security.CurrentUser com.insurai.model.User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        // Privileged roles (SUPER_ADMIN, COMPANY_ADMIN, AGENT) may query on behalf of a specified userId.
        // Self-only for standard USERs (ignores unprivileged attempt to query another user's IDOR param).
        boolean isPrivileged = currentUser.getRole() != null && 
            ("SUPER_ADMIN".equalsIgnoreCase(currentUser.getRole()) || 
             "COMPANY_ADMIN".equalsIgnoreCase(currentUser.getRole()) || 
             "AGENT".equalsIgnoreCase(currentUser.getRole()));
        Long targetUserId = (isPrivileged && userId != null) ? userId : currentUser.getId();
        
        List<SmartReminder> reminders = smartReminderService.getPendingReminders(targetUserId);
        return ResponseEntity.ok(reminders);
    }

    /**
     * Get all reminders for current user (privileged roles may pass optional userId to query on behalf of others).
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'SUPER_ADMIN', 'COMPANY_ADMIN')")
    public ResponseEntity<?> getAllReminders(
            @RequestParam(required = false) Long userId,
            @com.insurai.security.CurrentUser com.insurai.model.User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        boolean isPrivileged = currentUser.getRole() != null && 
            ("SUPER_ADMIN".equalsIgnoreCase(currentUser.getRole()) || 
             "COMPANY_ADMIN".equalsIgnoreCase(currentUser.getRole()) || 
             "AGENT".equalsIgnoreCase(currentUser.getRole()));
        Long targetUserId = (isPrivileged && userId != null) ? userId : currentUser.getId();

        List<SmartReminder> reminders = smartReminderService.getAllReminders(targetUserId);
        return ResponseEntity.ok(reminders);
    }

    /**
     * Mark reminder as sent/read (requires ownership or privileged role).
     */
    @PutMapping("/{reminderId}/mark-sent")
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'SUPER_ADMIN', 'COMPANY_ADMIN')")
    public ResponseEntity<?> markAsSent(
            @PathVariable Long reminderId,
            @com.insurai.security.CurrentUser com.insurai.model.User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        SmartReminder reminder = smartReminderRepository.findById(java.util.Objects.requireNonNull(reminderId)).orElse(null);
        if (reminder == null) {
            return ResponseEntity.notFound().build();
        }

        // Ownership check: Users can only mark their own reminders sent, unless holding a privileged admin/agent role.
        boolean isPrivileged = currentUser.getRole() != null && 
            ("SUPER_ADMIN".equalsIgnoreCase(currentUser.getRole()) || 
             "COMPANY_ADMIN".equalsIgnoreCase(currentUser.getRole()) || 
             "AGENT".equalsIgnoreCase(currentUser.getRole()));
        if (reminder.getUser() != null && !reminder.getUser().getId().equals(currentUser.getId()) && !isPrivileged) {
            return ResponseEntity.status(403).body("Access denied: You can only update your own reminders.");
        }

        smartReminderService.markAsSent(reminderId);
        return ResponseEntity.ok("Reminder marked as sent");
    }

    /**
     * Delete reminder (requires ownership or privileged role).
     */
    @DeleteMapping("/{reminderId}")
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'SUPER_ADMIN', 'COMPANY_ADMIN')")
    public ResponseEntity<?> deleteReminder(
            @PathVariable Long reminderId,
            @com.insurai.security.CurrentUser com.insurai.model.User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        SmartReminder reminder = smartReminderRepository.findById(java.util.Objects.requireNonNull(reminderId)).orElse(null);
        if (reminder == null) {
            return ResponseEntity.notFound().build();
        }

        // Ownership check: Users can only delete their own reminders, unless holding a privileged admin/agent role.
        boolean isPrivileged = currentUser.getRole() != null && 
            ("SUPER_ADMIN".equalsIgnoreCase(currentUser.getRole()) || 
             "COMPANY_ADMIN".equalsIgnoreCase(currentUser.getRole()) || 
             "AGENT".equalsIgnoreCase(currentUser.getRole()));
        if (reminder.getUser() != null && !reminder.getUser().getId().equals(currentUser.getId()) && !isPrivileged) {
            return ResponseEntity.status(403).body("Access denied: You can only delete your own reminders.");
        }

        smartReminderService.deleteReminder(reminderId);
        return ResponseEntity.ok("Reminder deleted");
    }
}
