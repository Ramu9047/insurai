package com.insurai.controller;

import com.insurai.config.DataSeeder;
import com.insurai.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ResetController – wipes all application data and re-seeds fresh demo data.
 *
 * POST /api/admin/reset-database
 * → requires ADMIN or SUPER_ADMIN role
 * → deletes every table in dependency-safe order
 * → invokes DataSeeder to repopulate immediately
 *
 * USE WITH CAUTION – this is destructive and irreversible.
 */
@RestController
@RequestMapping("/api/admin")
public class ResetController {

    private static final Logger logger = LoggerFactory.getLogger(ResetController.class);

    private final AuditLogRepository auditLogRepo;
    private final FeedbackRepository feedbackRepo;
    private final ClaimRepository claimRepo;
    private final UserCompanyMapRepository userCompanyMapRepo;
    private final UserPolicyRepository userPolicyRepo;
    private final BookingRepository bookingRepo;
    private final PolicyRepository policyRepo;
    private final UserRepository userRepo;
    private final CompanyRepository companyRepo;
    private final DataSeeder dataSeeder;

    public ResetController(
            AuditLogRepository auditLogRepo,
            FeedbackRepository feedbackRepo,
            ClaimRepository claimRepo,
            UserCompanyMapRepository userCompanyMapRepo,
            UserPolicyRepository userPolicyRepo,
            BookingRepository bookingRepo,
            PolicyRepository policyRepo,
            UserRepository userRepo,
            CompanyRepository companyRepo,
            DataSeeder dataSeeder) {
        this.auditLogRepo = auditLogRepo;
        this.feedbackRepo = feedbackRepo;
        this.claimRepo = claimRepo;
        this.userCompanyMapRepo = userCompanyMapRepo;
        this.userPolicyRepo = userPolicyRepo;
        this.bookingRepo = bookingRepo;
        this.policyRepo = policyRepo;
        this.userRepo = userRepo;
        this.companyRepo = companyRepo;
        this.dataSeeder = dataSeeder;
    }

    /**
     * Full database wipe + reseed.
     * Admin / Super-Admin only.
     */
    @PostMapping("/reset-database")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<Map<String, Object>> resetDatabase() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            logger.info("ResetController: Starting full database wipe...");

            // Delete in reverse dependency order (children before parents)
            auditLogRepo.deleteAll();
            feedbackRepo.deleteAll();
            claimRepo.deleteAll();
            userCompanyMapRepo.deleteAll();
            userPolicyRepo.deleteAll();
            bookingRepo.deleteAll();

            // Nullify user→company FK before deleting companies / users
            userRepo.findAll().forEach(u -> {
                if (u.getCompany() != null) {
                    u.setCompany(null);
                    userRepo.save(u);
                }
            });

            policyRepo.deleteAll();
            companyRepo.deleteAll();
            userRepo.deleteAll();

            logger.info("All tables cleared. Re-seeding...");

            // Re-seed
            dataSeeder.run();

            result.put("success", true);
            result.put("message", "Database reset and reseeded successfully.");
            result.put("timestamp", java.time.LocalDateTime.now().toString());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("❌ ResetController error: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Health / status check – doesn't modify data.
     */
    @GetMapping("/reset-database/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("users", userRepo.count());
        info.put("companies", companyRepo.count());
        info.put("policies", policyRepo.count());
        info.put("bookings", bookingRepo.count());
        info.put("userPolicies", userPolicyRepo.count());
        info.put("claims", claimRepo.count());
        info.put("feedback", feedbackRepo.count());
        info.put("auditLogs", auditLogRepo.count());
        return ResponseEntity.ok(info);
    }
}
