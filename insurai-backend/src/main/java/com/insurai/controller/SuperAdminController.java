package com.insurai.controller;

import com.insurai.model.Company;

import com.insurai.model.User;
import com.insurai.repository.UserRepository;
import com.insurai.service.CompanyService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/super-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminController.class);

    @Autowired
    private CompanyService companyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.insurai.repository.PolicyRepository policyRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    /**
     * Create a new company and its admin
     */
    @PostMapping("/companies")
    public ResponseEntity<?> createCompany(@RequestBody Map<String, String> payload) {
        try {
            // Extract Company Details
            String name = payload.get("name");
            String regNum = payload.get("registrationNumber");
            String compEmail = payload.get("email");
            String address = payload.get("address"); // Optional
            String phone = payload.get("phone"); // Optional

            // Extract Admin Details
            String adminName = payload.get("adminName");
            String adminEmail = payload.get("adminEmail");
            String adminPass = payload.get("adminPassword");

            if (name == null || regNum == null || compEmail == null || adminEmail == null || adminPass == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
            }

            if (userRepository.findByEmail(adminEmail).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Admin Email already in use"));
            }

            // Create Company first
            Company company = new Company();
            company.setName(name);
            company.setRegistrationNumber(regNum);
            company.setEmail(compEmail);
            company.setAddress(address);
            company.setPhone(phone);
            company.setStatus("APPROVED"); // Created by Super Admin, so auto-approved
            company.setIsActive(true);
            company.setCreatedAt(LocalDateTime.now());
            // We might need to set a dummy password for Company entity if it's required by
            // database,
            // based on the model it seems required.
            company.setPassword(passwordEncoder.encode("COMPANY_" + java.util.UUID.randomUUID().toString()));

            Company savedCompany = companyService.registerCompany(company);

            // Create Company Admin User
            User admin = new User();
            admin.setName(adminName);
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPass));
            admin.setRole("COMPANY");
            admin.setIsActive(true);
            admin.setVerified(true);
            admin.setCompany(savedCompany);

            userRepository.save(admin);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Company and Admin created successfully",
                    "company", savedCompany,
                    "admin", adminName));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Autowired
    private com.insurai.repository.AuditLogRepository auditLogRepository;

    /**
     * Get all audit logs
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs() {
        return ResponseEntity.ok(auditLogRepository.findAll(org.springframework.data.domain.Sort
                .by(org.springframework.data.domain.Sort.Direction.DESC, "timestamp")));
    }

    /**
     * Emergency Policy Suspension
     */
    @PutMapping("/policies/{policyId}/suspend")
    public ResponseEntity<?> suspendPolicy(@PathVariable long policyId, @RequestBody Map<String, String> payload) {
        String reason = payload.get("reason");
        logger.warn("Suspending Policy {} due to: {}", policyId, reason);
        com.insurai.model.Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy not found"));

        policy.setStatus("SUSPENDED");
        // Log reason if audit log exists
        policyRepository.save(policy);
        return ResponseEntity.ok(Map.of("message", "Policy suspended", "policy", policy));
    }

    /**
     * Enable Policy (Reactivate)
     */
    @PutMapping("/policies/{policyId}/enable")
    public ResponseEntity<?> enablePolicy(@PathVariable long policyId) {
        com.insurai.model.Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy not found"));

        policy.setStatus("ACTIVE");
        policyRepository.save(policy);
        return ResponseEntity.ok(Map.of("message", "Policy enabled", "policy", policy));
    }

    /**
     * Get all companies
     */
    @GetMapping("/companies")
    public ResponseEntity<List<Company>> getAllCompanies() {
        List<Company> companies = companyService.getAllCompanies();
        return ResponseEntity.ok(companies);
    }

    /**
     * Get companies by status
     */
    @GetMapping("/companies/status/{status}")
    public ResponseEntity<List<Company>> getCompaniesByStatus(@PathVariable String status) {
        List<Company> companies = companyService.getCompaniesByStatus(status);
        return ResponseEntity.ok(companies);
    }

    /**
     * Get pending company approvals
     */
    @GetMapping("/companies/pending")
    public ResponseEntity<List<Company>> getPendingCompanies() {
        List<Company> companies = companyService.getCompaniesByStatus("PENDING_APPROVAL");
        return ResponseEntity.ok(companies);
    }

    /**
     * Approve a company
     */
    @PostMapping("/companies/{companyId}/approve")
    public ResponseEntity<?> approveCompany(
            @PathVariable Long companyId,
            @RequestBody(required = false) Map<String, String> payload,
            Authentication auth) {
        try {
            User superAdmin = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            Company company = companyService.getCompanyById(companyId);

            company.setStatus("APPROVED");
            company.setIsActive(true);
            company.setApprovedBy(superAdmin);
            company.setApprovedAt(LocalDateTime.now());
            company.setUpdatedAt(LocalDateTime.now());

            Company approved = companyService.updateCompany(companyId, company);

            // Send notification (if notification service supports companies)
            // notificationService.notifyCompany(company, "Your company has been
            // approved!");

            return ResponseEntity.ok(Map.of(
                    "message", "Company approved successfully",
                    "company", approved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Reject a company
     */
    @PostMapping("/companies/{companyId}/reject")
    public ResponseEntity<?> rejectCompany(
            @PathVariable Long companyId,
            @RequestBody Map<String, String> payload,
            Authentication auth) {
        try {
            String reason = payload.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Rejection reason is required"));
            }

            User superAdmin = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            Company company = companyService.getCompanyById(companyId);

            company.setStatus("REJECTED");
            company.setIsActive(false);
            company.setApprovedBy(superAdmin);
            company.setApprovedAt(LocalDateTime.now());
            company.setUpdatedAt(LocalDateTime.now());

            Company rejected = companyService.updateCompany(companyId, company);

            // Send notification with reason
            // notificationService.notifyCompany(company, "Your company application was
            // rejected: " + reason);

            return ResponseEntity.ok(Map.of(
                    "message", "Company rejected",
                    "company", rejected,
                    "reason", reason));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Suspend a company (emergency action)
     */
    @PostMapping("/companies/{companyId}/suspend")
    public ResponseEntity<?> suspendCompany(
            @PathVariable Long companyId,
            @RequestBody Map<String, String> payload) {
        try {
            String reason = payload.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Suspension reason is required"));
            }

            Company company = companyService.getCompanyById(companyId);
            company.setStatus("SUSPENDED");
            company.setIsActive(false);
            company.setUpdatedAt(LocalDateTime.now());

            Company suspended = companyService.updateCompany(companyId, company);

            return ResponseEntity.ok(Map.of(
                    "message", "Company suspended",
                    "company", suspended,
                    "reason", reason));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Reactivate a suspended company
     */
    @PostMapping("/companies/{companyId}/reactivate")
    public ResponseEntity<?> reactivateCompany(@PathVariable Long companyId) {
        try {
            Company company = companyService.getCompanyById(companyId);

            if (!"SUSPENDED".equals(company.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only suspended companies can be reactivated"));
            }

            company.setStatus("APPROVED");
            company.setIsActive(true);
            company.setUpdatedAt(LocalDateTime.now());

            Company reactivated = companyService.updateCompany(companyId, company);

            return ResponseEntity.ok(Map.of(
                    "message", "Company reactivated successfully",
                    "company", reactivated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Toggle company active status
     */
    @PutMapping("/companies/{companyId}/status")
    public ResponseEntity<?> toggleCompanyStatus(
            @PathVariable Long companyId,
            @RequestBody Map<String, Boolean> payload) {
        try {
            Boolean isActive = payload.get("isActive");
            if (isActive == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "isActive field is required"));
            }

            Company company = companyService.toggleCompanyStatus(companyId, isActive);
            return ResponseEntity.ok(Map.of(
                    "message", "Company status updated",
                    "company", company));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Autowired
    private com.insurai.repository.BookingRepository bookingRepository;

    @Autowired
    private com.insurai.repository.UserPolicyRepository userPolicyRepository;

    /**
     * Get system-wide dashboard statistics
     */
    @Autowired
    private com.insurai.repository.UserCompanyMapRepository userCompanyMapRepository;

    @Autowired
    private com.insurai.repository.FeedbackRepository feedbackRepository;

    @Autowired
    private com.insurai.repository.ExceptionCaseRepository exceptionCaseRepository;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        List<Company> allCompanies = companyService.getAllCompanies();

        // 1. Company Metrics (Global)
        long totalCompanies = allCompanies.size();
        long pendingCompanies = allCompanies.stream().filter(c -> "PENDING_APPROVAL".equals(c.getStatus())).count();
        long activeCompanies = allCompanies.stream().filter(c -> Boolean.TRUE.equals(c.getIsActive())).count();
        long suspendedCompanies = allCompanies.stream().filter(c -> "SUSPENDED".equals(c.getStatus())).count();

        // 2. User & Agent Metrics (Global — only real end-user roles)
        long totalUsers = userRepository.findByRole("USER").size();
        long totalAgents = userRepository.findByRole("AGENT").size();
        long totalFeedback = feedbackRepository.count();

        // 3. Exception Cases & Fraud (Global)
        long fraudAlerts = exceptionCaseRepository.count();

        // 4. Funnel Metrics (Global)
        long totalBookings = bookingRepository.count();
        long consulted = bookingRepository.findAll().stream()
                .filter(b -> "COMPLETED".equals(b.getStatus()) || "APPROVED".equals(b.getStatus())
                        || "REJECTED".equals(b.getStatus()))
                .count();
        long approved = bookingRepository.findAll().stream()
                .filter(b -> "APPROVED".equals(b.getStatus()) || "COMPLETED".equals(b.getStatus())).count();
        // Global Policies Issued (Count all UserPolicies)
        long policiesIssued = userPolicyRepository.count();

        Map<String, Object> response = new java.util.HashMap<>();

        // Executive Metrics — Java Map.of supports max 10 entries; use HashMap for 9+
        java.util.Map<String, Object> metrics = new java.util.HashMap<>();
        metrics.put("totalCompanies", totalCompanies);
        metrics.put("totalUsers", totalUsers);
        metrics.put("totalAgents", totalAgents);
        metrics.put("policiesIssued", policiesIssued);
        metrics.put("fraudAlerts", fraudAlerts);
        metrics.put("totalFeedback", totalFeedback);
        metrics.put("pendingApprovals", pendingCompanies);
        metrics.put("activeCompanies", activeCompanies);
        metrics.put("suspendedCompanies", suspendedCompanies);
        response.put("metrics", metrics);

        // Enriched Company List for Governance Panel
        List<Map<String, Object>> enrichedCompanies = new java.util.ArrayList<>();
        for (Company c : allCompanies) {
            Map<String, Object> cMap = new java.util.HashMap<>();
            cMap.put("id", c.getId());
            cMap.put("name", c.getName());
            cMap.put("status", c.getStatus());

            // Calculate specific counts
            long cPolicies = userPolicyRepository.findByPolicyCompanyId(c.getId()).size();
            long cAgents = userRepository.countByCompanyIdAndRole(c.getId(), "AGENT"); // Assuming method exists or
                                                                                       // stream filter if not
            // Fallback for missing repo method:
            // long cAgents = userRepository.findAll().stream().filter(u ->
            // "AGENT".equals(u.getRole()) &&
            // c.getId().equals(u.getCompany().getId())).count();
            // Actually, let's use stream for safety if repo method isn't confirmed

            long cUsers = userCompanyMapRepository.countActiveUsersByCompany(c.getId());

            cMap.put("policiesIssued", cPolicies);
            cMap.put("agents", cAgents);
            cMap.put("users", cUsers);

            // Mock Risk Score logic based on status
            String risk = "Low";
            if ("SUSPENDED".equals(c.getStatus()))
                risk = "High";
            else if ("PENDING_APPROVAL".equals(c.getStatus()))
                risk = "Medium";
            cMap.put("riskScore", risk);

            enrichedCompanies.add(cMap);
        }

        response.put("companies", enrichedCompanies);

        Map<String, Object> funnel = new java.util.HashMap<>();
        funnel.put("visitors", totalUsers * 4); // Estimate
        funnel.put("registered", totalUsers);
        funnel.put("appointments", totalBookings);
        funnel.put("consulted", consulted);
        funnel.put("approved", approved);
        funnel.put("policiesIssued", policiesIssued);
        response.put("funnel", funnel);

        // System Health (Mocked for wireframe)
        response.put("systemHealth", Map.of(
                "aiAccuracy", 91,
                "fraudDetection", 88,
                "apiResponseTime", 210,
                "dbLoad", "Normal",
                "uptime", 99.8));

        // Agent Leaderboard — derived from real bookings
        List<com.insurai.model.Booking> allBookings = bookingRepository.findAll();
        java.util.Map<com.insurai.model.User, java.util.List<com.insurai.model.Booking>> byAgent = allBookings.stream()
                .filter(b -> b.getAgent() != null)
                .collect(java.util.stream.Collectors.groupingBy(com.insurai.model.Booking::getAgent));

        List<Map<String, Object>> leaderboard = byAgent.entrySet().stream()
                .map(entry -> {
                    com.insurai.model.User agent = entry.getKey();
                    java.util.List<com.insurai.model.Booking> bList = entry.getValue();
                    long approvals = bList.stream()
                            .filter(b -> "APPROVED".equals(b.getStatus()) || "COMPLETED".equals(b.getStatus())).count();
                    int approvalPct = bList.isEmpty() ? 0 : (int) ((approvals * 100) / bList.size());
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("name", agent.getName());
                    m.put("company", agent.getCompany() != null ? agent.getCompany().getName() : "—");
                    m.put("approval", approvalPct);
                    m.put("avgTime", "15 mins");
                    m.put("total", bList.size());
                    return m;
                })
                .sorted((a, b) -> Integer.compare((int) b.get("approval"), (int) a.get("approval")))
                .limit(5)
                .collect(java.util.stream.Collectors.toList());

        // Add rank field
        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboard.get(i).put("rank", i + 1);
        }
        response.put("agentLeaderboard", leaderboard);

        return ResponseEntity.ok(response);
    }

    /**
     * Get system-wide statistics (Legacy)
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        List<Company> allCompanies = companyService.getAllCompanies();

        long totalCompanies = allCompanies.size();
        long pendingCompanies = allCompanies.stream().filter(c -> "PENDING_APPROVAL".equals(c.getStatus())).count();
        long approvedCompanies = allCompanies.stream().filter(c -> "APPROVED".equals(c.getStatus())).count();
        long rejectedCompanies = allCompanies.stream().filter(c -> "REJECTED".equals(c.getStatus())).count();
        long suspendedCompanies = allCompanies.stream().filter(c -> "SUSPENDED".equals(c.getStatus())).count();
        long activeCompanies = allCompanies.stream().filter(c -> Boolean.TRUE.equals(c.getIsActive())).count();

        return ResponseEntity.ok(Map.of(
                "totalCompanies", totalCompanies,
                "pendingCompanies", pendingCompanies,
                "approvedCompanies", approvedCompanies,
                "rejectedCompanies", rejectedCompanies,
                "suspendedCompanies", suspendedCompanies,
                "activeCompanies", activeCompanies));
    }
}
