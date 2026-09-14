package com.insurai.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "user_policies", indexes = {
    @Index(name = "idx_user_policy_user", columnList = "user_id"),
    @Index(name = "idx_user_policy_status", columnList = "status"),
    @Index(name = "idx_user_policy_workflow", columnList = "workflow_status")
})
public class UserPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "policy_id")
    private Policy policy;

    private LocalDate startDate;
    private LocalDate endDate;
    private String status; // QUOTED, PENDING_APPROVAL, APPROVED, REJECTED, PAYMENT_PENDING, ACTIVE,
                           // EXPIRED
    private String pdfUrl; // Specific signed doc
    private String recommendationNote;

    // Workflow Enhancement Fields
    private String agentNotes; // Agent's consultation notes
    private String rejectionReason; // Why policy was rejected
    @ElementCollection
    private java.util.List<Long> alternativePolicyIds = new java.util.ArrayList<>(); // Suggested alternatives
    private String workflowStatus; // CONSULTATION_PENDING, CONSULTATION_COMPLETED, APPROVED, REJECTED

    private java.time.LocalDateTime purchasedAt;
    private Double premium;

    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    public String getRecommendationNote() {
        return recommendationNote;
    }

    public void setRecommendationNote(String recommendationNote) {
        this.recommendationNote = recommendationNote;
    }

    public String getAgentNotes() {
        return agentNotes;
    }

    public void setAgentNotes(String agentNotes) {
        this.agentNotes = agentNotes;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public java.util.List<Long> getAlternativePolicyIds() {
        return alternativePolicyIds;
    }

    public void setAlternativePolicyIds(java.util.List<Long> alternativePolicyIds) {
        this.alternativePolicyIds = alternativePolicyIds;
    }

    public String getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(String workflowStatus) {
        this.workflowStatus = workflowStatus;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    @PrePersist
    protected void onCreate() {
        startDate = LocalDate.now();
        endDate = startDate.plusYears(1); // Default 1 year validity
        if (status == null)
            status = "ACTIVE";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.time.LocalDateTime getPurchasedAt() {
        return purchasedAt;
    }

    public void setPurchasedAt(java.time.LocalDateTime purchasedAt) {
        this.purchasedAt = purchasedAt;
    }

    public Double getPremium() {
        return premium;
    }

    public void setPremium(Double premium) {
        this.premium = premium;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
