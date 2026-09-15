package com.insurai.controller;

import com.insurai.dto.AgentReviewDecisionDTO;
import com.insurai.dto.PolicyPurchaseWorkflowDTO;
import com.insurai.service.PolicyPurchaseWorkflowService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Policy Purchase Workflow Controller
 * Manages the complete policy purchase flow with human-in-the-loop
 */
@RestController
@RequestMapping("/api/policy-workflow")
public class PolicyPurchaseWorkflowController {

    private final PolicyPurchaseWorkflowService workflowService;

    public PolicyPurchaseWorkflowController(PolicyPurchaseWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    /**
     * Step 1: User requests consultation for a specific policy
     * POST /api/policy-workflow/request-consultation
     */
    @PostMapping("/request-consultation")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PolicyPurchaseWorkflowDTO> requestConsultation(
            @RequestBody Map<String, Object> request) {

        Long userId = Long.valueOf(request.get("userId").toString());
        Long policyId = Long.valueOf(request.get("policyId").toString());
        String reason = request.get("reason") != null ? request.get("reason").toString() : null;

        PolicyPurchaseWorkflowDTO workflow = workflowService.requestPolicyConsultation(
                userId, policyId, reason);

        return ResponseEntity.ok(workflow);
    }

    /**
     * Get user's workflow history
     * GET /api/policy-workflow/user/{userId}
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'SUPER_ADMIN', 'COMPANY_ADMIN')")
    public ResponseEntity<List<PolicyPurchaseWorkflowDTO>> getUserWorkflows(
            @PathVariable Long userId) {

        List<PolicyPurchaseWorkflowDTO> workflows = workflowService.getUserWorkflows(userId);
        return ResponseEntity.ok(workflows);
    }

    /**
     * Step 2: Agent gets pending reviews
     * GET /api/policy-workflow/agent/{agentId}/pending
     */
    @GetMapping("/agent/{agentId}/pending")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<List<PolicyPurchaseWorkflowDTO>> getAgentPendingReviews(
            @PathVariable Long agentId) {

        List<PolicyPurchaseWorkflowDTO> pending = workflowService.getAgentPendingReviews(agentId);
        return ResponseEntity.ok(pending);
    }

    /**
     * Step 3: Agent makes decision (Approve/Reject)
     * POST /api/policy-workflow/agent/{agentId}/review/{bookingId}
     */
    @PostMapping("/agent/{agentId}/review/{bookingId}")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<PolicyPurchaseWorkflowDTO> agentReview(
            @PathVariable Long agentId,
            @PathVariable Long bookingId,
            @RequestBody AgentReviewDecisionDTO decision) {

        PolicyPurchaseWorkflowDTO result = workflowService.agentReviewDecision(
                bookingId, agentId, decision);

        return ResponseEntity.ok(result);
    }

    /**
     * Step 4: Admin approval for high-risk cases
     * POST /api/policy-workflow/admin/{adminId}/approve/{bookingId}
     */
    @PostMapping("/admin/{adminId}/approve/{bookingId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPANY_ADMIN')")
    public ResponseEntity<PolicyPurchaseWorkflowDTO> adminApproval(
            @PathVariable Long adminId,
            @PathVariable Long bookingId,
            @RequestBody Map<String, String> request) {

        String notes = request.get("notes");
        PolicyPurchaseWorkflowDTO result = workflowService.adminApproval(
                bookingId, adminId, notes);

        return ResponseEntity.ok(result);
    }

    /**
     * Get workflow details by ID
     * GET /api/policy-workflow/{workflowId}
     */
    @GetMapping("/{workflowId}")
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'SUPER_ADMIN', 'COMPANY_ADMIN')")
    public ResponseEntity<PolicyPurchaseWorkflowDTO> getWorkflowDetails(
            @PathVariable Long workflowId) {

        List<PolicyPurchaseWorkflowDTO> workflows = workflowService.getUserWorkflows(workflowId);
        if (workflows.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(workflows.get(0));
    }
}
