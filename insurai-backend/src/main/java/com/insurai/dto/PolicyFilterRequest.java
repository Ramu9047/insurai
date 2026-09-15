package com.insurai.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Policy Filter Request DTO
 */
public class PolicyFilterRequest {
    @Size(max = 50, message = "Type cannot exceed 50 characters")
    private String type;

    @Size(max = 50, message = "Category cannot exceed 50 characters")
    private String category;

    @PositiveOrZero(message = "Min premium must be positive or zero")
    private Double minPremium;

    @PositiveOrZero(message = "Max premium must be positive or zero")
    private Double maxPremium;

    @PositiveOrZero(message = "Min coverage must be positive or zero")
    private Double minCoverage;

    @PositiveOrZero(message = "Max coverage must be positive or zero")
    private Double maxCoverage;

    private List<String> features;

    @Size(max = 50, message = "Sort field cannot exceed 50 characters")
    private String sortBy;

    @Size(max = 10, message = "Sort order cannot exceed 10 characters")
    private String sortOrder;

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getMinPremium() {
        return minPremium;
    }

    public void setMinPremium(Double minPremium) {
        this.minPremium = minPremium;
    }

    public Double getMaxPremium() {
        return maxPremium;
    }

    public void setMaxPremium(Double maxPremium) {
        this.maxPremium = maxPremium;
    }

    public Double getMinCoverage() {
        return minCoverage;
    }

    public void setMinCoverage(Double minCoverage) {
        this.minCoverage = minCoverage;
    }

    public Double getMaxCoverage() {
        return maxCoverage;
    }

    public void setMaxCoverage(Double maxCoverage) {
        this.maxCoverage = maxCoverage;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
}
