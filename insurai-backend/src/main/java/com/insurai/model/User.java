package com.insurai.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_role", columnList = "role"),
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_is_active", columnList = "isActive")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password;
    private String role;

    private Boolean available = false;
    private Boolean verified = false;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private String resetToken;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private java.time.LocalDateTime resetTokenExpiry;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private String verificationToken;

    private Boolean isActive = true; // For Admin activation/deactivation

    @Transient
    @com.fasterxml.jackson.annotation.JsonProperty("companyId")
    private Long mappingCompanyId; // Renamed to avoid JPA property collision

    // Profile Fields
    private Integer age;
    private String phone;
    private Double income;
    private Integer dependents;
    private String healthInfo;
    private String address;

    // Agent Specific Fields
    private Double rating = 4.5; // Default 4.5 stars
    private String specialization; // Health, Life, Motor
    private String bio; // "Top agent with 10 years experience..."

    // Agent Governance Fields
    @ElementCollection
    private java.util.List<String> assignedRegions = new java.util.ArrayList<>(); // Regions agent can serve

    @ElementCollection
    private java.util.List<String> assignedPolicyTypes = new java.util.ArrayList<>(); // Policy types agent can handle

    private String deactivationReason; // Reason for deactivation (if isActive = false)
    private java.time.LocalDateTime deactivatedAt; // When agent was deactivated

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company; // Linked Company for COMPANY role, or agent's employer company

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Double getIncome() {
        return income;
    }

    public void setIncome(Double income) {
        this.income = income;
    }

    public Integer getDependents() {
        return dependents;
    }

    public void setDependents(Integer dependents) {
        this.dependents = dependents;
    }

    public String getHealthInfo() {
        return healthInfo;
    }

    public void setHealthInfo(String healthInfo) {
        this.healthInfo = healthInfo;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public java.util.List<String> getAssignedRegions() {
        return assignedRegions;
    }

    public void setAssignedRegions(java.util.List<String> assignedRegions) {
        this.assignedRegions = assignedRegions;
    }

    public java.util.List<String> getAssignedPolicyTypes() {
        return assignedPolicyTypes;
    }

    public void setAssignedPolicyTypes(java.util.List<String> assignedPolicyTypes) {
        this.assignedPolicyTypes = assignedPolicyTypes;
    }

    public String getDeactivationReason() {
        return deactivationReason;
    }

    public void setDeactivationReason(String deactivationReason) {
        this.deactivationReason = deactivationReason;
    }

    public java.time.LocalDateTime getDeactivatedAt() {
        return deactivatedAt;
    }

    public void setDeactivatedAt(java.time.LocalDateTime deactivatedAt) {
        this.deactivatedAt = deactivatedAt;
    }

    public Long getMappingCompanyId() {
        return mappingCompanyId;
    }

    public void setMappingCompanyId(Long mappingCompanyId) {
        this.mappingCompanyId = mappingCompanyId;
    }

    public java.time.LocalDateTime getResetTokenExpiry() {
        return resetTokenExpiry;
    }

    public void setResetTokenExpiry(java.time.LocalDateTime resetTokenExpiry) {
        this.resetTokenExpiry = resetTokenExpiry;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }
}
